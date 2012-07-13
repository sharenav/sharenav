#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=YOUR_SOURCEFORGE_USERNAME_HERE
numver=0.7.75
rcver=rc-v0.7.8
ver=$numver-map71

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant
cd Osm2GpsMid
#ant clean
ant
cd ..
cp -p dist/Osm2GpsMid-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

cd Osm2GpsMid
#ant clean
ant
cd ..
cp -p dist/Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar

# 


scp Osm2GpsMid-$ver.jar $user,gpsmid@web.sf.net:htdocs/prebuild/Osm2GpsMid-$ver-$rcver.jar
scp Osm2GpsMid-$ver-debug.jar $user,gpsmid@web.sf.net:htdocs/prebuild/Osm2GpsMid-$ver-$rcver-debug.jar
