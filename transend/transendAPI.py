#!/usr/bin/env python
#encoding: utf-8

from flask import Flask, render_template, Response, request, jsonify, redirect, url_for
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
from werkzeug.utils import secure_filename
import json as j
from bson.json_util import dumps
import base64


reload(sys)
sys.setdefaultencoding("utf8")

conf = read_properties_file(os.path.join('transend/transend/static/config'))
ip = conf["ip_address"]
port = conf["port"]
UPLOAD_FOLDER = '/home/ec2-user/transend/transend/static/fileuploads/'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])
basedir = os.path.abspath(os.path.dirname(__file__))
app = Flask(__name__)
socketio = SocketIO(app)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

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


@socketio.on('generate_qr_pc')
def connected_pc():
    logging.debug("pc_connect")
    generate_qr_pc(request.sid)


@socketio.on('generate_qr_phone')
def connected_phone(data):
    logging.debug("phone_connect")
    logging.debug(data)
    generate_qr_phone(request.sid, data)


def generate_qr_pc(session_id):
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
    emit("qrpath", {'QR': qr_path, 'direction': "to_pc"}, room=session_id, broadcast=True)
	
    logging.info("Emitted QR path: %s on session_id: %s", qr_path, session_id)

def generate_qr_phone(session_id, data):
    qr_name = id_generator()
    qr_path = make_qrpath(qr_name)
    set_qrpath(qr_name)
    passwd = id_generator()
    if session_id in sessions:
        qr_to_delete = sessions[session_id]["qr"]
        os.remove("/home/ec2-user/transend/transend/"+qr_to_delete)
    sessions[session_id] = {"password":passwd, "qr":qr_path}
    filename = str(data["name"])
    
    payload = {"password": passwd, "sessionid": session_id, "direction": "to_phone", "filename": filename, "size": data['size']}
    img = qrcode.make(payload)
    img.save("/home/ec2-user/transend/transend/"+qr_path)
    emit("qrpath", {'QR': qr_path, 'direction': "to_phone"}, room=session_id, broadcast=True)
        
    logging.info("Emitted QR path: %s on session_id: %s", qr_path, session_id)

@socketio.on('authentication_phone')
def authorise_phone(json):
    password = json['password']
    session_id = json['sessionid']
    filename = json['filename']
    emit("scanned", {'scanned':True}, room=session_id, broadcast=True)
    size = json['size']
    filedir = app.config['UPLOAD_FOLDER'] + session_id
    updir = os.path.join(basedir, filedir)
    file_dir = os.path.join(updir, filename)
    sent_password = sessions[session_id]["password"].encode("utf-8")
    if sent_password in password:
	with open(file_dir, "rb") as f:
	    filestr = f.read()
	    base64file = base64.encodestring(filestr)
            emit("receivefile", {'filebytes': base64file , 'size': size, 'filename': filename})
    shutil.rmtree(updir)
    logging.info("Deleted %s", file_dir)
#    os.remove("/home/ec2-user/transend/transend/"+qrpath)
#    logging.info("Deleted %s", qrpath) 


@socketio.on('authentication')
def authorise(json):
    password = json['password']
    session_id = json['sessionid']
    logging.debug("Password: %s", password)
    logging.debug("Session_id: %s", session_id)
    sent_password = sessions[session_id]["password"].encode("utf-8")
    if sent_password in password:
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
    if 'sessionid' in payload:
        session_id = payload['sessionid'].encode('utf8')
        if session_id not in sessions:
            logging.error("Session Expired: %s", session_id)
            return
        if "url" in payload:
            url = payload['url']
            logging.info("Got URL")
            emit("link", {'url': url}, room=session_id)
            logging.info("Emitted URL %s", url)
        else:
            logging.info("Got request to write payload")
            filename = payload['filename'].encode('utf8')
            logging.info("Attempting to write file %s", filename)
            content = payload['content']
            download_dir = os.path.join("/home/ec2-user/transend/transend/static/downloads/",session_id)
            if not os.path.exists(download_dir):
                os.makedirs(download_dir)
            fullpath = os.path.join(download_dir, filename)
            f = open(fullpath, 'w')
            f.write(content)
            logging.info("Wrote file successfully")
            emit("file", {'session_id': session_id, 'filename': filename}, room=session_id)
            logging.info("Emitted file %s", filename)
	    emit("filewritten", {'written':True})
#        del sessions[session_id]
#        logging.info("Deleted session %s", session_id)
    else:
        logging.error("No session in payload")


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
#    os.remove("/home/ec2-user/transend/transend/"+qrpath)
#    logging.info("Deleted %s", qrpath)
    return resp

@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'GET':
        logging.info("Rendering template...")
        return render_template('index.html')


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/uploader/<session>', methods = ['POST'])
def upldfile(session):
    if request.method == 'POST':
        files = request.files['file']
        if files:
            filename = secure_filename(files.filename)
	    filedir = app.config['UPLOAD_FOLDER'] + session
	    updir = os.path.join(basedir, filedir)
    	    if not os.path.exists(updir):
            	os.makedirs(updir)
	    print(updir)
            files.save(os.path.join(updir, filename))
            file_size = os.path.getsize(os.path.join(updir, filename))

            return jsonify(name=filename, size=file_size)

	
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
