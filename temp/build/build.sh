#!/bin/bash -x

#
# This script will create the native code. It can only run on MacOS
# since that is the only system that has no proper cross compile. Before
# you run this script, ensure the dc.sh has created the shell scripts
# to start the docker cross tool chain commands.
#
# This script must be executed from the root of the project so that the
# docker image can see all the directories
#


# $1 - toolchain script
# $2 - name of library
# $3 - native path
function cross {
    rm -rf  src/main/c/build/*
    mkdir src/main/c/build
    build/$1 bash -c 'cd src/main/c/build;rm *;cmake ..;make'
    mkdir -p `dirname native/$3`
    cp src/main/c/build/$2 native/$3
}

if [ `uname` != Darwin ]
then 
    echo This script must run on MacOS to be able to also generate binaries for MacOS
    exit 1
fi


rm -rf native/*
mkdir -p native

cross ""                        libhello.dylib                  darwin/x64/hello.dylib
cross dc/windows-shared-x64     libhello.dll                    win/x64/hello.dll
cross dc/linux-armv5            libhello.so                     linux/armv5/libhello.so
cross dc/linux-armv7            libhello.so                     linux/armv7/libhello.so
cross dc/linux-arm64            libhello.so                     linux/arm64/libhello.so
cross dc/linux-x64              libhello.so                     linux/x64/libhello.so
cross dc/linux-x86              libhello.so                     linux/x86/libhello.so

