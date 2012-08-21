#!/bin/sh

# usage: bundlemap.sh mapdir [ sourcejarname ] targetjarname

# written to be run in main GpsMid directory

# needed for signing android .apk; for Android, mapdir must be named "assets"
# todo: deduce from environment

jarsigner=jarsigner
#jarsigner=/usr/lib/jvm/default-java/bin/jarsigner

# uncomment to enable android signing with a real key
#passparam="-keystore /some/where.jks -storepass IWontTell"

if [ "$1" = "-a" ]
then
    android="y"
    shift
fi

ver=0.7.99-map72
andtarget=android-full-connected-outlines
midtarget=full-connected

if [ "$#" != 2 -a "$#" != 1 ]
then
  echo "usage: bundlemap.sh [ -a ] mapdir [ targetversion ]"
  echo "where -a means bundle for Android, and targetversion is"
  echo 'e.g. "android-online" for Android or e.g. "editing" or "full" for J2ME'
  echo 'defaults are: android-online for Android, editing for J2ME'.
  echo "example: \"bundlemap.sh -a map\" will take dist/Gpsmid-Generic-$andtarget-$ver.apk, bundle map from dir \"map\" into it,"
  echo "and write file Gpsmid-Generic-$andtarget-$ver.apk which can be installed on a device." 
  exit 1
fi >&2


if [ "$android" ]
then

  if [ "$2" ]
  then
    andtarget="$2"
  fi

  apk=`ls -t dist/GpsMid-Generic-$andtarget-0*.apk 2>/dev/null|head -1`
  if [ "$apk" ]
  then
     ver=`echo $apk|sed "s/dist\/GpsMid-Generic-$andtarget-//" | sed "s/\.apk//"`
  fi
  mid=dist/GpsMid-Generic-$andtarget-$ver.apk
else
  if [ "$2" ]
  then
    midtarget="$2"
  fi

  jar=`ls -t dist/GpsMid-Generic-$midtarget-0*.jar 2>/dev/null|head -1`
  if [ "$jar" ]
  then
     ver=`echo $jar|sed "s/dist\/GpsMid-Generic-$midtarget-//" | sed "s/\.jar//"`
  fi
  mid=dist/GpsMid-Generic-$midtarget-$ver.jar
fi

midtarget=`echo $mid | sed 's/^dist\///'`

#echo "mid: $mid"
#echo "midtarget: $midtarget"
#echo "ver: $ver"
#exit 1

if [ "$1" ]
then
  mapdir="$1"
fi

if [ ! -d "$mapdir" ]
then
  echo "Error: map dir $mapdir doesn't exist or is not a directory"
  exit 1
fi


if [ "$mid" != "$midtarget" ]
then
  cp "$mid" "$midtarget"
fi

#jar uf  "$midtarget" `find .`

zip -q "$midtarget" -d META-INF/
zip -q "$midtarget" -d c/\* t\*/\* s\*.d dict\*.dat d\*/\*.d names\*.dat legend.dat
#cp -p "$midtarget"  "$midtarget".copy
#if [ "$android" ]
#then
  unzip -q -o "$mid" META-INF/\*
#fi

if [ "$android" ]
then
    zip "$midtarget" -u `find $mapdir|grep -v META-INF`
else
    cd "$mapdir"
    zip ../"$midtarget" -u `find |grep -v META-INF|sed 's/^.\///'`
    cd ..
fi

zip -q "$midtarget" -d META-INF
zip -q "$midtarget" -d META-INF/MANIFEST.MF
zip -q "$midtarget" -Z store -u META-INF
zip -q "$midtarget" -Z store -u META-INF/MANIFEST.MF

if [ "$android" ]
then

  # tools/bundlemap-sign-android.sh "`basename $midtarget`"

  $jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA $passparam $midtarget gpsmid

fi
