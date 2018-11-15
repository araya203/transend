#!/usr/bin/env python
#encoding: utf-8

from flask import Flask, render_template, Response, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import random
import string
import qrcode
import os
import sys
import ntpath
import shutil
from config import read_properties_file
import socket

reload(sys)
sys.setdefaultencoding("utf8")

conf = read_properties_file(os.path.join('transend/transend/static/config'))
ip = conf["ip_address"]
port = conf["port"]
app = Flask(__name__)
socketio = SocketIO(app)
app.config['SECRET_KEY'] = 'mysecret'
sessions = {}

logging.basicConfig(
    filename='/home/ec2-user/transend/transend/log/transend.log',
    level='DEBUG',
    format="[%(asctime)s.%(msecs)03d][%(levelname)s][%(module)s]-[%(funcName)s]: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


def make_qrpath(qr_name):
    return "static/QR/"+qr_name+".png"


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
    generate_qr(request.sid)


def generate_qr(session_id):
    qr_name = id_generator()
    qr_path = make_qrpath(qr_name)
    set_qrpath(qr_name)
    passwd = id_generator()
    if session_id in sessions:
        qr_to_delete = sessions[session_id]["qr"]
        os.remove("/home/ec2-user/transend/transend/"+qr_to_delete)
    sessions[session_id] = {"password":passwd, "qr":qr_path}
    payload = {"password": passwd, "sessionid": session_id}
    img = qrcode.make(payload)
    img.save("/home/ec2-user/transend/transend/"+qr_path)
    emit("qrpath", {'QR': qr_path}, room=session_id, broadcast=True)
    logging.info("Emitted QR path: %s on session_id: %s", qr_path, session_id)


@socketio.on('authentication')
def authorise(json):
    password = json['password']
    session_id = json['sessionid']
    logging.debug("Password: %s", password)
    logging.debug("Session_id: %s", session_id)
    if sessions[session_id]["password"] in password:
        payload = {"authorisation": "Authorised", "session_id": session_id}
        logging.info("Authorised")
        logging.info("Got request to load")
        emit("loading", {'isloading': True}, room=session_id)
        logging.info("Emitted loading is True")

    else:
        payload = {"authorisation": "Denied", "session_id": session_id}
        logging.error("Access Denied. No such session or password incorrect: %s", sessions[session_id])
    emit("decision", payload)
    logging.info("Emitted descision")


@socketio.on('payload')
def handle_content(payload):
    if "url" in payload:
        url = payload['url']
        logging.info("Got URL")
        session_id = payload['sessionid'].encode('utf8')
        emit("link", {'url': url}, room=session_id)
        logging.info("Emitted URL %s", url)
    else:
        logging.info("Got request to write payload")
        session_id = payload['sessionid'].encode('utf8')
        filename = payload['filename'].encode('utf8')
        logging.info("Attempting to write file %s", filename)
        content = payload['content']
        if session_id not in sessions:
            logging.error("Session Expired: %s", session_id)
            return
        download_dir = os.path.join("/home/ec2-user/transend/transend/static/downloads/",session_id)
        if not os.path.exists(download_dir):
            os.makedirs(download_dir)
        fullpath = os.path.join(download_dir, filename)
        f = open(fullpath, 'w')
        f.write(content)
        logging.info("Wrote file successfully")
        emit("loading", {'isloading': False}, room=session_id)
        emit("file", {'session_id': session_id, 'filename': filename}, room=session_id)
        logging.info("Emitted file %s", filename)
        logging.info("Emitted loading is False")
        del sessions[session_id]
        logging.info("Deleted session %s", session_id)

	emit("filewritten", {'written':True})


@app.route('/getfile/<session>/<file_name>')
def get_output_file(session, file_name):
    file_dir = "/home/ec2-user/transend/transend/static/downloads/"+session
    file_path = file_dir+"/"+file_name
    if not os.path.isfile(file_path):
        logging.error("%s is not a file", file_path)
        
    logging.info("Attempting to download %s", file_path)

    with open(file_path, 'rb') as f:
        resp = Response(f.read())
    file_name = ntpath.basename(file_path)
    resp.headers["Content-Disposition"] = "attachment; filename={0}".format(file_name)
    resp.headers["Content-type"] = "application/octet-stream"
    logging.info("Downloaded Successfully")
    shutil.rmtree(file_dir)
    logging.info("Deleted %s", file_path)
    os.remove("/home/ec2-user/transend/transend/"+qrpath)
    logging.info("Deleted %s", qrpath)
    return resp


@app.route('/')
def index():
    logging.info("Rendering template...")
    return render_template('index.html')

@app.route('/test')
def test():
    logging.info("Rendering template...")
    return render_template('test.html')


@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404


@app.errorhandler(403)
def page_not_found(e):
    return render_template('403.html'), 403


@app.errorhandler(410)
def page_not_found(e):
    return render_template('410.html'), 410


def run():
    if __name__ == '__main__':
        socketio.run(app, debug=True, host=ip, port=port)

run()
