from flask import Flask, render_template, Response, request
from flask_socketio import SocketIO, send,emit
import random
import string
import qrcode
import os
from config import read_properties_file
import socket


conf = read_properties_file('static/config')
ip = conf["ip_address"]
port = conf["port"]
print(socket.gethostname())
app = Flask(__name__)
socketio = SocketIO(app)
def id_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))
#app.config['SERVER_NAME'] = "206.225.94.205:5000"
QR_FOLDER = os.path.join('static', 'QR')
#app.config['QR_FOLDER'] = QR_FOLDER
app.config['SECRET_KEY'] = 'mysecret'
print(ip, port)

sessions = {}

@socketio.on('browserconnect')
def connected():
	qrpath = "static/QR/"+id_generator()+".png"
	passwd = id_generator()
	print(QR_FOLDER)
	sessions[request.sid] = passwd
	print("CONNECTED: " + request.sid)
	print("CONNECTED: " + passwd)
	payload = {"password": passwd, "sessionid": request.sid}
	img = qrcode.make(payload)
	img.save(qrpath)
	emit("qrpath", {'QR': qrpath}, room=request.sid, broadcast=True)

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
	print(QR_FOLDER)
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
    return resp


if __name__ == '__main__':
	socketio.run(app, debug=True, host=ip, port=port)
