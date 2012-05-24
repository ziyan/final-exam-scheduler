#!/bin/bash
# benchmark different algorithms
# this script uses ./algo.bash
#
# @author Ziyan Zhou (zxz6862)
#
ALGO="SA Genetic Permutation"

for A in $ALGO; do
./algo.bash $A
done

