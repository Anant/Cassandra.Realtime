import json
from datetime import datetime
import os
import pathlib
from confluent_kafka import avro
import traceback
import pandas as pd
from pandas import json_normalize

working_dir = pathlib.Path().absolute()
absolute_path_to_schema = os.path.join(working_dir, "../kafka", "leaves-record-schema.avsc")

value_schema = None
schema_dict = None

with open(absolute_path_to_schema, "r") as f:
    schema_dict = json.load(f)
    value_schema_str = json.dumps(schema_dict)
    # TODO do we have to do this if schema is in schema registry already? Or is this just a check within this job before we send it?
    value_schema = avro.loads(value_schema_str)

def normalize_record(record):
    """
    use pandas to normalize json. 
    Might not be necessary for when self.data_type is json, but was used for when it was excel
    """
    message_df = pd.DataFrame(json_normalize(record))
    ### jr = message_df.drop([excel_topic_column_name], axis=1)
    json_str = message_df.to_json(orient='records')

    return json_str

def dict_to_avro(dictionary):
    """
    NOTE this is not necessary when using AvroProducer
    - Would need to add more mappings if we used more fields that used unions
    """
    avro_dict = dictionary
    py_to_avro_mapping = {
        type(None): "null",
        str: "string",
        list: "array",
        int: "int",
    }


    for field in schema_dict["fields"]:
        # check schema for unions (where more than one is allowed). If union, specify type
        # http://avro.apache.org/docs/current/spec.html#Unions
        is_union = type(field["type"]) == list
        field_name = field["name"]
        record_value = dictionary.get(field_name, None)
        record_field_type = type(record_value)

        if is_union:
            # check the value for this record, and mark accordingly
            avro_type_for_record_field = py_to_avro_mapping[record_field_type]

            avro_dict[field_name] = {avro_type_for_record_field: record_value}
        else:
            avro_dict[field_name] = record_value

    return avro_dict
            
def fit_record_to_schema(record, **kwargs):
    """
    takes record dict and into message according to our schema
    - Currently only have to convert timestamps into unix timestamps
        Support only for one format: "2019-08-02T21:45:18Z"

    NOTE currently mutates original record. 

    TODO iterate over schema once per job, rather than once per record (?). Small performance boost.
    """
    # check all fields for logical type of timestamp-millis
    timestamp_fields = []

    for field_def in schema_dict["fields"]:
        field_name = field_def["name"]
        field_type = None
        if type(field_def["type"]) == dict:
            field_type = field_def["type"]["type"]
        else:
            field_type = field_def["type"]

        logical_type = field_def.get("logicalType", None)

        if logical_type == "timestamp-millis":
            timestamp_fields.append(field_name)

        # check if record has field
        if record.get(field_name, None) is None:

            # should work whether field_type is str or list (?) but hopefully no one is setting their field type to just a string called "null"!
            if "null" not in field_type:
                print(f"\n\nDANGER: {field_name} is None\n\n")
                raise Exception(f"field {field_name} is not defined for record {record['id']}, stopping right there")

    for field_name in timestamp_fields:
        # if currently value is string, try to convert to unix timestamp, with millisecond precision
        record_field_value = record[field_name]
        if type(record_field_value) == str:
            dt_obj = datetime.strptime(record_field_value, '%Y-%m-%dT%H:%M:%SZ')
            millisec = int(dt_obj.timestamp() * 1000)
            record[field_name] = millisec

    if kwargs.get("debug_mode", False):
        # if record has extra field, throw error
        check_record_for_extra_field(record)

    return record


def prepare_record(record, data_type):
    if data_type == "excel":
        # create alias for record, "row" 
        # row = record
        # row.replace(['\u200b'], '')
        pass

    elif data_type == "json":
        # don't need to do anything yet
        return record

def import_excel(properties):
    """
    NOTE not currently in use
    """
    # excel_sheet_name = properties['SHEET_NAME']
    # sheets = pd.read_excel(properties["file_name"], sheet_name=properties["excel_sheet_name"])
    pass

def import_json(properties):
    data_json = None
    data_file_name = properties['DATA_FILE_NAME']
    with open(data_file_name) as read_file:
        data_json = json.load(read_file)

    # dig into the json to get the list we want
    path_to_items_str = properties['PATH_TO_ITEMS']
    path_to_items_list = path_to_items_str.split(".")

    # traverse the path
    data = data_json
    for segment in path_to_items_list:
        data = data[segment]

    # data should now be list of dicts, and each dict will be a message to send to kafka
    records = data
    return records

################################################
# debugging helpers (can remove if remove all method calls)
################################################
def check_record_for_extra_field(record):
    """
    checks if record has a field that schema doesn't have
    """
    for key in record.keys():
        match = False
        for field_def in schema_dict["fields"]:
            if field_def["name"] == key:
                match = True
                break

        if not match:
            raise Exception(f"Record {record['id']} has extra field: {key}")

def schema_fields_for_record(record):
    """
    for this record, produce the fields that it has and their types, for comparison with our actual schema
    presumably, if we wanted to we could also produce a schema with something similar to this. But not doing that for now
    """
    schema_fields = []
    for key, value in record.items():
        field_type = type(value).__name__
        schema_fields.append(
            {
                "name": key,
                "type": field_type,
            }
        )

    return schema_fields
