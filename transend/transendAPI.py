#encoding: utf-8

from flask import Flask, render_template, Response, request
from flask_socketio import SocketIO, emit
import logging
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
sessions = {}

logging.basicConfig(
        filename='log/transend.log',
        level='DEBUG',
        format="[%(asctime)s.%(msecs)03d][%(levelname)s][%(module)s]-[%(funcName)s]: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )


def set_qrpath(unique_code):
    global qrpath
    qrpath = "static/QR/"+unique_code+".png"
    logging.debug("QR Path: %s", qrpath)

def get_qrpath():
    return qrpath

def id_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


@socketio.on('browserconnect')
def connected():
    qrpath = set_qrpath(id_generator())
    passwd = id_generator()
    sessions[request.sid] = passwd
    payload = {"password": passwd, "sessionid": request.sid}
    img = qrcode.make(payload)
    img.save(get_qrpath())
    emit("qrpath", {'QR': get_qrpath()}, room=request.sid, broadcast=True)
    logging.info("Emitted QR path: %s on session_id: %s", get_qrpath(), request.sid)


@socketio.on('authentication')
def authorise(json):
    password = json['password']
    session_id = json['sessionid']
    logging.debug("Password: %s", password)
    logging.debug("Session_id: %s", session_id)
    if sessions[session_id] in password:
        payload = {"authorisation": "Authorised", "session_id": session_id}
	logging.info("Authorised")
    else:
        payload = {"authorisation": "Denied", "session_id": session_id}
	logging.error("Access Denied. No such session or password incorrect: %s", sessions[session_id])
    emit("decision", payload)
    logging.info("Emitted descision")

@socketio.on('sendingstatus')
def isloading(json):
    logging.info("Got request to load")
    emit("loading", {'isloading': True}, room=json['sessionid'])
    logging.info("Emitted loading is True")


@socketio.on('payload')
def handle_content(newdata):
    logging.info("Got request to write payload")
    session_id = newdata['sessionid'].encode('utf8')
    filename = newdata['filename'].encode('utf8')
    logging.info("Attempting to write file %s", filename)
    content = newdata['content']
    if session_id not in sessions:
        logging.error("Session Expired: %s", session_id)
        return
    f = open(filename, 'w')
    f.write(content)
    logging.info("Wrote file successfully")
    emit("file", {'filename': filename}, room=session_id)
    logging.info("Emitted file %s", filename)
    del sessions[session_id]
    logging.info("Deleted session %s", session_id)


@app.route('/')
def index():
    logging.info("Rendering template...")
    return render_template('index.html', IP=ip, PORT=port)


@app.route('/getfile/<name>')
def get_output_file(name):
    file_name = os.path.join(name)
    if not os.path.isfile(file_name):
	logging.error("%s is not a file", file_name)
        return None
    logging.info("Attempting to download %s", file_name)

    # read without gzip.open to keep it compressed
    with open(file_name, 'rb') as f:
        resp = Response(f.read())
    # set headers to tell encoding and to send as an attachment
    resp.headers["Content-Disposition"] = "attachment; filename={0}".format(file_name)
    resp.headers["Content-type"] = "application/octet-stream"
    logging.info("Downloaded Successfully")
    os.remove(file_name)
    logging.info("Deleted %s", file_name)
    os.remove(qrpath)
    logging.info("Deleted %s", qrpath)
    return resp


if __name__ == '__main__':
    socketio.run(app, host=ip, port=port)
