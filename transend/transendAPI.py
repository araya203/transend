from flask import Flask, render_template, Response, request
from flask_socketio import SocketIO, send,emit
import random
import string
import qrcode
import os
from config import read_properties_file

conf = read_properties_file('static/config')
ip = conf["ip_address"]
port = conf["port"]

print(ip, port)
app = Flask(__name__)
socketio = SocketIO(app)
def id_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))
app.config['SERVER_NAME'] = ip + ":" + port
QR_FOLDER = os.path.join('static', 'QR', id_generator()+".png")
app.config['QR_FOLDER'] = QR_FOLDER
app.config['SECRET_KEY'] = 'mysecret'

sessions = {}
#/qrpath = QR_FOLDER+"/"+id_generator()+".png"
@socketio.on('connect')
def connected():
	passwd = id_generator()
	print(QR_FOLDER)
	sessions[request.sid] = passwd
	print(passwd, request.sid)
	img = qrcode.make(passwd+" "+request.sid)
	img.save(QR_FOLDER)
	
@socketio.on('authentication')
def authorise(json):
    password = json['password']
    session_id = json['sessionid']
    print(password, session_id)
    if sessions[session_id] in password:
        payload = {"authorisation": "Authorised", "session_id": session_id}
    else:
        payload = {"authorisation": "Denied", "session_id": session_id}
    emit("decision", payload)

@socketio.on('payload')
def handle_content(json):
	session_id = json['sessionid']
	filename = json['filename']
	content = json['content']
	f = open(filename, 'w')
	f.write(content)
	emit("file", {'filename': filename}, room=session_id, broadcast=True)
	del sessions[session_id]

@app.route('/')
def index():
	print(QR_FOLDER)
	return render_template('index.html', QR=QR_FOLDER, IP=ip, PORT=port)


@app.route('/getfile/<name>')
def get_output_file(name):
    file_name = os.path.join(name)
    if not os.path.isfile(file_name):
       abort(404)
    # read without gzip.open to keep it compressed
    with open(file_name, 'rb') as f:
        resp = Response(f.read())
    # set headers to tell encoding and to send as an attachment
    resp.headers["Content-Disposition"] = "attachment; filename={0}".format(file_name)
    resp.headers["Content-type"] = "application/octet-stream"
    os.remove(file_name)
    return resp


if __name__ == '__main__':
    socketio.run(app, host=ip, port=port)
