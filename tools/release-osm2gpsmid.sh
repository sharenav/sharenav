#!/bin/sh
#
# update the Osm2ShareNav snapshots
#

user=PUT_YOUR_SOURCEFORGE_USERNAME_HERE
numver=0.7.5
ver=$numver-map66

ant clean
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 

ant
cd Osm2ShareNav
ant clean
ant
cd ..
cp -p Osm2ShareNav/dist/Osm2ShareNav-$ver.jar .

# debug version build
ant clean
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish

cd Osm2ShareNav
ant clean
ant
cd ..
cp -p Osm2ShareNav/dist/Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar

# 


mkdir "Release $numver"
cp dist/*-$ver.jar README.mkd WHATSNEW.txt Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar "Release $numver"

#scp Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar $user,sharenav@web.sf.net:htdocs/prebuild

chmod -R g+w "Release $numver"

# FIXME update upload location

# scp -p -r "Release $numver" $user,sharenav@web.sf.net:/home/frs/project/g/gp/sharenav/sharenav/
