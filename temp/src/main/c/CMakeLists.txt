cmake_minimum_required(VERSION 3.0.0)
project(hello)
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s")
 
include_directories(include)
 
file(GLOB SOURCES "src/*.c")
 
#Generate the shared library from the sources
add_library(hello SHARED ${SOURCES})
 

 