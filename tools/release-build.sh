#!/bin/bash
#
# build Osm2GpsMid & GpsMid midlets for signing
#

numver=0.8.1
ver=$numver-map72
map=Berlin

ant clean
( cd Osm2GpsMid ; ant clean )

#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant

ant -propertyfile android.properties android

cd Osm2GpsMid

# ant clean

ant

cd ..

for target in full-connected full midsize minimal blackberry
do
  java -Xmx1024m -jar dist/Osm2GpsMid-$ver.jar --nogui --properties=Osm2GpsMid/mapdef/$map-$target
done

for target in full-connected full midsize minimal
do
  java -Xmx1024m -jar dist/Osm2GpsMid-$ver.jar --nogui --properties=Osm2GpsMid/mapdef/$map-android-$target
done

cp -p dist/Osm2GpsMid-$ver.jar .

mkdir "Release $numver"
mkdir "Release $numver/debug"

cp GpsMid-Generic-{blackberry,full-connected,full,midsize,minimal}-$ver.{ja?,apk} README.mkd WHATSNEW.txt Osm2GpsMid-$ver.jar "Release $numver"

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry

ant debug j2mepolish
ant -propertyfile android.properties debug android

cd Osm2GpsMid
ant

cd ..

cp -p dist/Osm2GpsMid-$ver.jar "Release $numver/Osm2GpsMid-$ver-debug.jar"

pwd

for target in full-connected full midsize minimal blackberry
do
  java -Xmx1024m -jar dist/Osm2GpsMid-$ver.jar --nogui --properties=Osm2GpsMid/mapdef/$map-$target
done

for target in full-connected full midsize minimal
do
  java -Xmx1024m -jar dist/Osm2GpsMid-$ver.jar --nogui --properties=Osm2GpsMid/mapdef/$map-android-$target
done

mkdir debug
for i in Gps*-$ver.{ja?,apk}
do
   mv $i debug
done

cp debug/GpsMid-Generic-{blackberry,full-connected,full,midsize,minimal}-$ver.{ja?,apk} "Release $numver/debug"

chmod -R g+w "Release $numver"
