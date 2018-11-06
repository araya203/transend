#encoding: utf-8

from flask import Flask, render_template, Response, request
from flask_socketio import SocketIO, emit
import random
import string
import qrcode
import os
import sys
from config import read_properties_file
import socket

reload(sys)
sys.setdefaultencoding("utf8")
conf = read_properties_file('static/config')
ip = conf["ip_address"]
port = conf["port"]
app = Flask(__name__)
socketio = SocketIO(app)
# app.config['SERVER_NAME'] = "206.225.94.205:5000"
app.config['SECRET_KEY'] = 'mysecret'
print(ip, port)
sessions = {}

def set_qrpath(unique_code):
    global qrpath
    qrpath = "static/QR/"+unique_code+".png"

def get_qrpath():
    return qrpath

def id_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


@socketio.on('browserconnect')
def connected():
    qrpath = set_qrpath(id_generator())
    passwd = id_generator()
    sessions[request.sid] = passwd
    print("CONNECTED: " + request.sid)
    print("CONNECTED: " + passwd)
    payload = {"password": passwd, "sessionid": request.sid}
    img = qrcode.make(payload)
    img.save(get_qrpath())
    emit("qrpath", {'QR': get_qrpath()}, room=request.sid, broadcast=True)


@socketio.on('authentication')
def authorise(json):
    password = json['password']
    session_id = json['sessionid']
    print(password, session_id)
    if sessions[session_id] in password:
        payload = {"authorisation": "Authorised", "session_id": session_id}
    else:
        payload = {"authorisation": "Denied", "session_id": session_id}
    print(payload)
    emit("decision", payload)

@socketio.on('sendingstatus')
def isloading(json):
    emit("loading", {'isloading': True}, room=json['sessionid'])


@socketio.on('payload')
def handle_content(newdata):
    print("HANDLE CONTENT")
    session_id = newdata['sessionid'].encode('utf8')
    filename = newdata['filename'].encode('utf8')
    content = newdata['content']
    print(session_id, filename)
    if session_id not in sessions:
        return
    f = open(filename, 'w')
    f.write(content)
    emit("file", {'filename': filename}, room=session_id)
    del sessions[session_id]


@app.route('/')
def index():
    return render_template('index.html', IP=ip, PORT=port)


@app.route('/getfile/<name>')
def get_output_file(name):
    file_name = os.path.join(name)
    if not os.path.isfile(file_name):
        return None
    # read without gzip.open to keep it compressed
    with open(file_name, 'rb') as f:
        resp = Response(f.read())
    # set headers to tell encoding and to send as an attachment
    resp.headers["Content-Disposition"] = "attachment; filename={0}".format(file_name)
    resp.headers["Content-type"] = "application/octet-stream"
    os.remove(file_name)
    os.remove(qrpath)
    return resp


if __name__ == '__main__':
    socketio.run(app, debug=True, host=ip, port=port)
