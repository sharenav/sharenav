#!/bin/sh

# usage: bundlemap.sh mapdir targetjarname

mid=dist/GpsMid-Generic-editing-0.6.3.jar

midtarget=target-0.6.3.jar

if [ "$1" ]
then
  mapdir="$1"
fi

if [ ! -d "$mapdir" ]
then
  echo "Error: map dir $mapdir doesn't exist or is not a directory"
  exit 1
fi

if [ "$2" ]
then
  midtarget="$2"
fi


cd "$mapdir" || exit 3

cp ../"$mid" ../"$midtarget"

#jar uf  ../"$midtarget" `find .`

zip -q ../"$midtarget" -d META-INF/
zip -q ../"$midtarget" -d c/\* t\*/\* s\*.d dict\*.dat d\*/\*.d names\*.dat legend.dat
#cp -p ../"$midtarget"  ../"$midtarget".copy
zip -q ../"$midtarget" -u  `find .|grep -v META-INF`
zip -q ../"$midtarget" -d META-INF/
zip -q ../"$midtarget" -d META-INF/MANIFEST.MF
zip -q ../"$midtarget" -u META-INF/
zip -q ../"$midtarget" -u META-INF/MANIFEST.MF
