from flask import Flask
from ExcelMsgPublishToKafka import sendXlsFileToKafka

app = Flask(__name__)

@app.route('/')
def index():
    return 'Hello, \ntry a GET to /xls to send an XLS file to kafka'


@app.route('/hello')
def indexHello():
    return 'Hello, World!'


@app.route('/xls')
def indexXls():
    sendXlsFileToKafka()
    return 'XLS file sent to kafka.'

if __name__ == '__main__':
    app.run(host="0.0.0.0", debug=False)
