import socket
import requests
import json
from time import sleep
import string
import random


def id_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


def isOpen(ip, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = s.connect_ex((ip, int(port)))
    if result == 0:
        s.close()
        return False
    else:
        s.close()
        return True


def get_open_ports():
    counter = 0
    ports = []
    for port in range(5001, 10000):
        if counter == 2:
            break
        if (isOpen('206.225.94.205', port)):
            ports.append(port)
            counter += 1

    return ports


def post_request():
    ports = get_open_ports()
    headers = {
        'Content-Type': 'application/json',
    }
    payload = {'port1': '{}'.format(ports[0]), 'port2': '{}'.format(ports[1])}

    url = 'http://206.225.94.205:5000/todo/api/v1.0/tasks'
    response = requests.post(url, headers=headers, data=json.dumps(payload))

    response.raise_for_status()
    sleep(5)
    return ports


ports = post_request()
print(ports)
socket = socket.socket()
print(ports[0])
socket.connect(("206.225.94.205", ports[0]))

print(ports[1])
passw = id_generator()
print(passw)
socket.send(passw)


name = socket.recv(1024)
print(name)
socket.send("gotname\r\n")
f = open(name, 'w')

encoding = socket.recv(1024)
while encoding:
    f.write(encoding)
    encoding = socket.recv(1024)

print("File Written")
f.close()
socket.close()
