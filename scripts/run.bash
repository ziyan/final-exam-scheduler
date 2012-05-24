#!/bin/bash
# script to run a FES program
# Usage: ./run.bash <algorithm> <K> <N>
# <algorithm> = Permutation|SA|Genetic
# <K> = number of cluster nodes to use, 0 means sequential
# <N> = data set to be used, data sets are located in data/, currently available: 5 10 20 40 50 100 200 2500
#
# @author Ziyan Zhou (zxz6862)
#

# arguments
A=$1
K=$2
N=$3
JVM_ARGS=$4

# location to store the outputs
DIR=performance/$A

# location to store the timing
TIMING=$DIR/timing.txt

# location to store program output
OUTPUT=$DIR/output.txt

# how many time to run
RUNS="0 1 2"

# settings
JDK=/usr/jdk/jdk1.5.0_17/bin
#JDK=/usr/jdk/jdk1.5.0_15/bin
JAVA=$JDK/java
JAVAC=$JDK/javac
JVM_ARGS="$JVM_ARGS -classpath ../src:../libs/pj.jar"

PJ_NP="-Dpj.np="

# create the output directory
mkdir -p $DIR

LINE="$N $K"
for RUN in $RUNS; do

PARAM="../data/$N/courses.txt ../data/$N/students.txt ../data/$N/relationships.txt $DIR/schedule.$K.$N.$RUN.txt"

if [ "$K" = "0" ]; then
	echo "$JAVA $JVM_ARGS FES${A}Seq $PARAM"
	RESULT="`$JAVA $JVM_ARGS FES${A}Seq $PARAM`"
else
	echo "$JAVA $PJ_NP$K $JVM_ARGS FES${A}Clu $PARAM"
	RESULT="`$JAVA $PJ_NP$K $JVM_ARGS FES${A}Clu $PARAM`"
fi
T="`echo $RESULT | awk '{print $1}'`"
LINE="$LINE	$T"
echo "$N	$K	$RESULT" >> $OUTPUT
echo "$N	$K	$RUN	$RESULT"

done
echo $LINE >> $TIMING
