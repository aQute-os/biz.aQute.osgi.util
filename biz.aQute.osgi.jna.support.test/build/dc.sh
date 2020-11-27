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
    docker run --rm dockcross/$1           > dc/$2
    chmod a+x dc/$2
}

rm -rf dc/*
mkdir dc

dc linux-armv5              linux-armv5l
dc linux-armv7              linux-armv7l
dc linux-arm64              linux-aarch64
dc linux-x64                linux-x86_64
dc linux-x86                linux-x86
dc windows-shared-x64       win-x86_64
dc windows-shared-x86       win-x86
