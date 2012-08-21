#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=YOUR_SOURCEFORGE_USERNAME_HERE

. ./android.properties

numver=0.7.98
rcver=rc-v0.8
ver=$numver-map72

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant

# normal android build
ant  -propertyfile android.properties android

cd Osm2GpsMid
#ant clean
ant
cd ..
cp -p dist/Osm2GpsMid-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

# debug android build
ant  -propertyfile android.properties debug android

cd Osm2GpsMid
#ant clean
ant
cd ..
cp -p dist/Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar

# 


scp Osm2GpsMid-$ver.jar $user,gpsmid@web.sf.net:htdocs/prebuild/Osm2GpsMid-$ver-$rcver.jar
scp Osm2GpsMid-$ver-debug.jar $user,gpsmid@web.sf.net:htdocs/prebuild/Osm2GpsMid-$ver-$rcver-debug.jar
