FROM python:3.8-slim-buster
RUN apt-get update

ENV PYTHONUNBUFFERED 1

RUN mkdir -p /usr/src/app

COPY ./requirement.txt /usr/src/app/requirement.txt
COPY ./flask-app.py /usr/src/app/
COPY ./config.ini /usr/src/app/
COPY ./data_importer.py /usr/src/app/
COPY ./assets /usr/src/app/

RUN pip3 install -r /usr/src/app/requirement.txt

WORKDIR /usr/src/app

CMD ["python3", "./flask-app.py"]
