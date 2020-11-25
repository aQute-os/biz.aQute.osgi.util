#!/bin/bash -x

#
# Create the shell scripts to run the dockcross toolchains.
# The shell scripts are created in dc
#

#
# Create a specific shell script
#
# $1 name of the type
#

function dc {
    docker run --rm dockcross/$1           > dc/$1
    chmod a+x dc/$1
}

rm -rf dc/*
mkdir dc

dc linux-armv7
dc linux-arm64
dc linux-armv5
dc linux-x64
dc linux-x86
dc windows-shared-x64
dc windows-shared-x86

echo "#!/bin/sh
\$@
" > dc/darwin-x64
chmod a+x dc/darwin-x64
