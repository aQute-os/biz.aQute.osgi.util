
from threading import Thread

def fun():
    print("Hello world")
    
    
t = Thread(fun)
t.start()
