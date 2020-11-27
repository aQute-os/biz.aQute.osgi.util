[native] JNA Support

These two projects provide support for using
JNA in OSGi. 

The native native code in Java is _JNI_. However, this is hard to so the JNA library that uses reflective 
access is very popular.

However, this makes it hard to understand what  happens. With JNI, the Java VM is in charge but JNA has its own 
dlopen/dlclose calls. Using the simple name of a library to open it will make that name permanent. 

The trick is to use the long path of where the dl resides. In OSGi, there is good support for 
storing the libraries in a bundle and the Bundle-NativeCode header to describe it.
However, JNA bypasses this. 

This project will provide a DynamicLibrary
class that will use the OSGi support to find
the library path. (This will make the 
framework extract it.) It will then load the
library by its path, not its simple name. If
it is closed, it will then also call
dlclose.

The test project shows how this should be used.
This project has native code in Git but there
are scripts to build the native code in docker
using dock cross, except for the mac. The
scripts are made for the mac. On another platform you need to comment out the 