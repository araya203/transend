# -*- coding: utf-8 -*-

import socket
import sys
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("port1", help="Port of PC")
parser.add_argument("port2", help="Port of Phone")
args = parser.parse_args()
port1 = int(args.port1)
port2 = int(args.port2)

socket_pc = socket.socket()
socket_ph = socket.socket()
socket_pc.bind(("", port1))
socket_ph.bind(("", port2))
socket_pc.listen(10) # Acepta hasta 10 conexiones entrantes.
socket_ph.listen(10) # Acepta hasta 10 conexiones entrantes.

pcsc, pcaddress = socket_pc.accept()
print(pcaddress)
phsc, phaddress = socket_ph.accept()
print(phaddress)

pass_from_pc = pcsc.recv(1024)
pass_from_phone = phsc.recv(1024)
print(pass_from_pc)
print(pass_from_phone)
message = "Authorised\r\n" if pass_from_pc == pass_from_phone else "Denied\r\n"
print(message)
phsc.send(message)

encoding = phsc.recv(1024)
phsc.send("Recieved. Sending to PC\r\n")
pcsc.send(encoding)

pcsc.close()
phsc.close()
socket_pc.close()
socket_ph.close()

