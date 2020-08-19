import json
def do_it():
    """
    grabs one or two records from the larger json file for easy testing
    """
    file_name = "data.json"
    data_json = None
    with open(file_name) as read_file:
        data_json = json.load(read_file)

    path_to_items_list = ["response", "docs"]
    data = data_json

    parent = None

    last_segment = path_to_items_list[-1]

    for segment in path_to_items_list:
        # get the parent so we can rewrite this easily
        # if this is last segment, cache first before setting it to data
        if segment == last_segment:
            parent = data

        data = data[segment]

    # grab two items and save that as sample
    print("original length", len(data))
    sample = data[0:2]

    # make sample dict, including all the frontmatter
    parent[last_segment] = sample
    print("final length", len(parent[last_segment]))

    # write to sample file
    with open("sample-data.json", "w") as write_file:
        json.dump(data_json, write_file)

    print("success. Using vim? Pretty print the new sample using jq: :%!jq .")

if __name__ == "__main__":
    do_it()
