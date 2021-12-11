#!/bin/sh
exec %java% %defines% -Dpid=$$ %jvmArgs% -cp "%classpath%" %main% "$@"
