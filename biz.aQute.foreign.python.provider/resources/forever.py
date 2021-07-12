import time
import sys

print("forever",file=sys.stderr)
sys.stderr.flush()

while True:
    print(".",file=sys.stderr)
    sys.stderr.flush()
    time.sleep(0.25)
    