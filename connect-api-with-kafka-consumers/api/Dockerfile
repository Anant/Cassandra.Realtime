FROM python:3.8-slim-buster
RUN apt-get update

ENV PYTHONUNBUFFERED 1

RUN mkdir -p /usr/src/app

COPY ./requirements.txt /usr/src/app/requirement.txt
COPY ./flask-app.py /usr/src/app/

# TODO check to make sure copying directories like this works
COPY ./astra.credentials /usr/src/app/

RUN pip3 install -r /usr/src/app/requirement.txt

WORKDIR /usr/src/app

CMD ["python3", "./flask-app.py"]
