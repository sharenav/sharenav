#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=YOUR_SOURCEFORGE_USERNAME_HERE
ver=0.7.32-map66

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant
cd Osm2GpsMid
ant clean
ant
cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

cd Osm2GpsMid
ant clean
ant
cd ..
cp -p Osm2GpsMid/dist/Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar

# 

scp Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar $user,gpsmid@web.sf.net:htdocs/prebuild
