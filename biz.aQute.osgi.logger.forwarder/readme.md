# Logger Forwarder

This bundle forwards logs from SLF4J to OSGi. A special bundle is needed because SLF4J can start logging
before the OSGi log service is present. This code maintains a static queue for log entries from 
SLF4J that is forwarded to the OSGi log service once it becomes available.

Although less popular, `java.util.logging` is also forwarded in this way.

**NOTE:** This bundle was designed to work with the bnd launcher, it uses the Embedded Activator support in the
bnd launcher and will not work with it.

In general, other log subsystems, like for example log4j, should be forwarded to SLF4J. It was considered to support
them directly from this JAR but the log4j APIs (old and new) are so badly designed that it is a major amount of work.
In general, the best solution is to minimimze logging to SLF4J. 

## How to Use

This is **not** a bundle. The JAR must be placed on the `-runpath` of a bnd(run) file to work. It will
export `slf4j` API which will look exported by the framework. That is, you should not add the SLF4J
API nor an implementation of SLF4J.

## Operation

The code implements the SLF4J extension mechanism to provide an implementation. That is, it provides the 
SLF4J LoggerFactory implementation.

When a new logger is requested, a _facade_ is returned. If the OSGi log service is present, the facade
will delegate to the OSGi log service. Otherwise, it will queue the log entries. When the OSGi log 
service becomes available, it will send all queued log entries to the OSGi service.

When the OSGi service is unregistered, the queuing will start again. That is, the OSGi log service
implementation can be updated.

## Configuration

This is not a bundle and therfore cannot be configured.