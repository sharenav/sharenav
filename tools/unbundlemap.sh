#!/bin/sh

# usage: unbundlemap.sh mapdir sourcejarname


if [ "$#" -ne 2 ]
then
  echo "Usage: unbundlemap mapdir sourcejarname"
  exit 1
fi

mapdir="$1"

sourcejar="$2"

if [ -e "$mapdir" ]
then
  echo "Error: $mapdir exists - if you want to create a map there, remove it first."
  exit 2
fi

if [ ! -r "$sourcejar" ]
then
  echo "Error: Cannot read $sourcejar"
  exit 2
fi


mkdir "$mapdir"

cd "$mapdir" || exit 3

jar xf ../"$sourcejar"
rm -r de net
rm *.loc
rm *.class
