#!/bin/bash
# FES Section Information Crawler for infocenter.rit.edu, works only on linux
# To put in file: ./crawler.bash | php crawler.php > courses.txt
# To upload to database: ./crawler.bash | php crawler.php | php upload.php <host> <username> <password> <dbname>
# See db.sql for script to create database
# This script output HTML to standard out
# @author Ziyan Zhou (zxz6862)
#

# settings
# path to wget
WGET="wget"

# course List URL
LIST="https://infocenter.rit.edu/INFO/COURSELIST/XSMBWEBM/SR085.STR?INIT=YES&CONVTOKEN=INIT"

# path to list of disciplines number
DISCIPLINES="disciplines"

# year to crawl
YEAR="2008"

# quarter to crawl
QUARTER="2"

# grab session token
TOKEN=`$WGET -q -O - $LIST | grep -m 1 '<input type="hidden" name="CONVTOKEN" value="' | awk -F\" '{print $6}'`
# construct URL
URL="https://infocenter.rit.edu/INFO/COURSELIST/XSMBWEBM/SR085.STR?INIT=YES&CONVTOKEN=INIT"
# construct post data
POSTDATA="YEAR=$YEAR&QUARTER=$QUARTER&INIT=NO&CONVTOKEN=$TOKEN"

# loop through all disciplines
for d in `cat $DISCIPLINES`; do
PAGE="01"
HASPAGE=""
# loop through all pages
while [[ ("$PAGE" != "") && ($HASPAGE == "") ]]; do
$WGET -q -O - --post-data "$POSTDATA&DISCIPLINE=$d&PAGE=$PAGE" $URL | php crawler.php $d
HASPAGE=`$WGET -q -O - --post-data "$POSTDATA&DISCIPLINE=$d&PAGE=$PAGE" $URL | grep -m 1 '&BUTE1;'`
PAGE=`$WGET -q -O - --post-data "$POSTDATA&DISCIPLINE=$d&PAGE=$PAGE" $URL | grep -m 1 '<input type="hidden" name="PAGE" value="' | awk -F\" '{print $6}'`
done
done

