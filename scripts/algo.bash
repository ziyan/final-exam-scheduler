#!/bin/bash
# script to benchmark an algorithm
# this script calls ./run.bash using different <K> number of nodes to use and different <N> problem sizes
# Usage: ./algo.bash <Permutation|SA|Genetic>
#
# @author Ziyan Zhou (zxz6862)
#
A=$1

# different K values for benchmark
KS="0 1 2 3 4 8 16"

# different Problem Size for benchmark
SIZES="5 10 20"

for N in $SIZES; do
for K in $KS; do

./run.bash $A $K $N

done
done
