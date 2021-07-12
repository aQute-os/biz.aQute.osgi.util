import json
import sys

class Gogo():
    def __init__(self, instream, outstream):
        self.instream = instream
        self.outstream = outstream
    def command(self, *args):
        self.outstream.write( " ".join(args)+"\n")
        self.outstream.flush()
        json_data = self.instream.readline()
        print("read " + json_data,file=sys.stderr)
        sys.stderr.flush()
        while json_data == None or json_data == "":
            print("got none " + json_data,file=sys.stderr)
            sys.stderr.flush()
            json_data = self.instream.readline()
        data = json.loads(json_data)
        return data


    
g = Gogo(sys.stdin, sys.stdout)
print("start python",file=sys.stderr)
sys.stderr.flush()

while True:
    r = g.command("test")
    assert r['value'] == 42