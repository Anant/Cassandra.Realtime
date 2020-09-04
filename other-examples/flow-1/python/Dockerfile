FROM python:3.8-slim-buster
RUN apt-get update

ENV PYTHONUNBUFFERED 1

RUN mkdir -p /usr/src/app

COPY ./requirement.txt /usr/src/app/requirement.txt
COPY ./flask-app.py /usr/src/app/
COPY ./ExcelKafkaConfig.ini /usr/src/app/
COPY ./ExcelMsgPublishToKafka.py /usr/src/app/
COPY ./KafkaTestMessages.xlsx /usr/src/app/

RUN pip3 install -r /usr/src/app/requirement.txt

WORKDIR /usr/src/app

CMD ["python3", "./flask-app.py"]