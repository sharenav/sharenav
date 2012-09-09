#!/bin/sh
#
# update the Osm2ShareNav snapshots
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

cd Osm2ShareNav
#ant clean
ant
cd ..
cp -p dist/Osm2ShareNav-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

# debug android build
ant  -propertyfile android.properties debug android

cd Osm2ShareNav
#ant clean
ant
cd ..
cp -p dist/Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar

# 


# FIXME update location

# scp Osm2ShareNav-$ver.jar $user,sharenav@web.sf.net:htdocs/prebuild/Osm2ShareNav-$ver-$rcver.jar
# scp Osm2ShareNav-$ver-debug.jar $user,sharenav@web.sf.net:htdocs/prebuild/Osm2ShareNav-$ver-$rcver-debug.jar
