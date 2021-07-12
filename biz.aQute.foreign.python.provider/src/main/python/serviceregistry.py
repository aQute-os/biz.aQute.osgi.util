from threading import Thread
from socket import  *
import sys

class Link(Thread):

    def __init__(self):
            Thread.__init__(self)
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  
              

    def open(self, host, port):
        self.sock.connect((host, port))
        self.start()
        
    def run(self):
        while True:
            ch = self.sock.recv(1)
            print(ch);
            sys.out.flush()

l = Link()
l.open("localhost", 29123)
 