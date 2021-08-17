# Python Support

This bundle provides support for running Python code that is highly integrated with the OSGi framework. This bundle
will start Python applications when it discovers a `python/app.py` file in a bundle. Before execution, the
`python` directory is copied to the bundle file area and the app is executed with that directory as working
directory.

The app is started if the bundle is started and will be killed when the bundle is stopped. If the app is terminated 
before the bundle is stopped, it will be restarted after a configurable delay.

This means that the Python code fully follows the bundle life cycle. In bndtools, this means that you can quite 
interactively develop your Python code.

## Communication

The app is started with stdin and stdout redirected to Gogo. Any line written to stdout will be executed as a
Gogo command. The result is written back as one line and encoded in JSON. It can be read in Python with a `readline` operation. This
looks like:

    import json
    import sys
    import time
    
    class Gogo():
        def __init__(self):
            self.instream = sys.stdin
            self.outstream = sys.stdout
            
        def command(self, gogoCmd):
            self.outstream.write( gogoCmd + "\n")
            self.outstream.flush()
            json_data = self.instream.readline()
            return json.loads(json_data)
        
    g = Gogo()
    
    while True:
        r = g.command("test")
        assert r['value'] == 42
        time.sleep(5)

The result has the following structure:

* `error` – Only present if there was an error, then value is not present
* `console` – Contains any console output, might be empty, never null.
* `value` – The returned value from the Gogo command. This is not always a dictionary, it can also be a number, boolean, or string.    

## Building

To create a Python bundle add the following to your bnd.bnd file, assuming your python code is the `python` directory of your project:

    -includeresource.python \
        python=python

The `python` directory should at minimum contain `app.py` file.

## Debugging

The communications occupies the use of stdin. Therefore, the Python code should _not_ print anything
to stdin, this will break the interface. Any output for debugging should be send to stderr. The stderr
stream is directly connected to the Java stderr. This will be generally be logged.


## Runtime Configuration

The Python Admin has a PID of `biz.aQute.foreign.python` can be configured with the following properties:

* `python` – The path to the appropriate Python command
* `restartDelay` – Number of milliseconds to delay between restarts

For testing purposes, the Python command path can also be set with the system property `biz.aQute.python.command`.


## Gogo

There is a `python:python` gogo command to see the running Python apps.

## TODO

* Process the requirements.txt file and install the requirements
* Provide some parameters in the manifest of the Python bundle
  * python version
  * if need to be restarted when exists
* Watchdog command to restart the python code when it no longer calls a command
* Restart command for a specific app
