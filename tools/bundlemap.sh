#!/bin/sh

# usage: bundlemap.sh mapdir [ sourcejarname ] targetjarname

# needed for signing android .apk
# todo: deduce from environment

jarsigner=/usr/lib/jvm/default-java/bin/

# uncomment to enable android signing with a real key
#passparam="-keystore /some/where.jks -storepass IWontTell"

mid=dist/GpsMid-Generic-editing-0.6.3.jar

midtarget=target-0.6.3.jar

if [ "$1" = "-a" ]
then
    android="y"
    shift
fi

if [ "$#" != 3 -a "$#" != 2 ]
then
  echo "usage: bundlemap.sh mapdir [ sourcejarname ] targetjarname"
  exit 1
fi

if [ "$2" ]
then
  mid="$2"
  midtarget="$2"
fi

if [ "$#" -eq 3 ]
then
    mid="$2"
    midtarget="$3"
fi


if [ "$1" ]
then
  mapdir="$1"
fi

if [ ! -d "$mapdir" ]
then
  echo "Error: map dir $mapdir doesn't exist or is not a directory"
  exit 1
fi


cd "$mapdir" || exit 3

if [ "$mid" != "$midtarget" ]
then
  cp ../"$mid" ../"$midtarget"
fi

#jar uf  "$midtarget" `find .`

zip -q ../"$midtarget" -d META-INF/
zip -q ../"$midtarget" -d c/\* t\*/\* s\*.d dict\*.dat d\*/\*.d names\*.dat legend.dat
#cp -p "$midtarget"  "$midtarget".copy
zip -q ../"$midtarget" -u  `find .|grep -v META-INF`
zip -q ../"$midtarget" -d META-INF/
zip -q ../"$midtarget" -d META-INF/MANIFEST.MF
zip -q ../"$midtarget" -u META-INF/
zip -q ../"$midtarget" -u META-INF/MANIFEST.MF


if [ "$android" ]
then

  cd ..
  # tools/bundlemap-sign-android.sh "`basename $midtarget`"

  $jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA $passparam GpsMid-Generic-android-online-0.7.76-map71.apk gpsmid

fi
