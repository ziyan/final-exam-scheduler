#!/bin/bash
# script to compile
# @author Ziyan Zhou (zxz6862)
#

JDK=/usr/jdk/jdk1.5.0_17/bin
#JDK=/usr/jdk/jdk1.5.0_15/bin
JAVA=$JDK/java
JAVAC=$JDK/javac
$JAVAC -classpath ../src:../libs/pj.jar ../src/*/*.java ../src/*.java
