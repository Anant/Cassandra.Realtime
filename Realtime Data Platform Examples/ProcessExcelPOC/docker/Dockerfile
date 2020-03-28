FROM python:3.8-slim-buster
ENV PYTHONUNBUFFERED 1
RUN mkdir -p /code
COPY requirement.txt /usr/src/app/requirement.txt
#RUN apk update && apk add bash gcc librdkafka-dev python3-dev
#WORKDIR /code
#ADD . /code/
#RUN pip3 install confluent-kafka

RUN apt-get update
RUN pip3 install -r /usr/src/app/requirement.txt



RUN mkdir -p /usr/src/app
#WORKDIR /usr/src/app
#RUN apk add --no-cache gcc musl-dev linux-headers
##COPY requirements.txt /usr/src/app
##RUN pip install --no-cache-dir -r /usr/src/app/requirements.txt
##RUN apk update && \
##    pip3 install --upgrade pip setuptools && \
##    pip3 install confluent-kafka
#RUN apk update && apk add --no-cache python3 librdkafka && \
#    python3 -m ensurepip && \
#    rm -r /usr/lib/python*/ensurepip && \
#    pip3 install --upgrade pip setuptools && \
#    pip3 install confluent-kafka && \
#    rm -r /root/.cache

COPY ../ExcelKafkaConfig.ini /usr/src/app/
COPY ../ExcelMsgPublishToKafka.py /usr/src/app/
COPY ../KafkaTestMessages.xlsx /usr/src/app/
CMD tail -f /dev/null