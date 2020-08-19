from flask import Flask
from JsonMsgPublishToKafka import sendJsonToKafka

app = Flask(__name__)

@app.route('/')
def index():
    return 'Hello, try a GET to /send-json to send a json data to kafka'

@app.route('/send-json')
def indexXls():
    sendJsonToKafka()
    return 'JSON data sent to kafka.'

if __name__ == '__main__':
    app.run(host="0.0.0.0", debug=False)
