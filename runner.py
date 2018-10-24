#!flask/bin/python
from flask import Flask, jsonify, request
import subprocess

app = Flask(__name__)

@app.route('/todo/api/v1.0/tasks', methods=['POST'])
def run_server():
    args = ("python", "/shared_homes/araya/transent-py/server.py","{}".format(request.json['port1']), "{}".format(request.json['port2']))
    popen = subprocess.Popen(args, stdout=subprocess.PIPE)
    return "done"

if __name__ == '__main__':
    app.run(host='172.16.4.108', port=5000)
