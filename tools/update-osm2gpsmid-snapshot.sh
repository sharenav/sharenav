#!/bin/sh
#
# update the Osm2ShareNav snapshots
#

user=SOURCEFORGE_USERNAME_HERE
ver=0.8.31-map72
. ./android.properties

ant clean

( cd Osm2ShareNav ; ant clean )
#
# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry

# normal build 


ant
ant -propertyfile android.properties android
cd Osm2ShareNav
ant
cd ..
cp -p dist/Osm2ShareNav-$ver.jar .

# debug version build
ant clean
( cd Osm2ShareNav ; ant clean )
ant debug -Ddevice=Generic/blackberry
ant debug j2mepolish
ant -propertyfile android.properties debug android

cd Osm2ShareNav
ant
cd ..
cp -p dist/Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar

ln -f -s Osm2ShareNav-$ver-debug.jar Osm2ShareNav-debug-latest.jar
ln -f -s Osm2ShareNav-$ver.jar Osm2ShareNav-latest.jar

# 

# FIXME update location

# ssh $user,sharenav@shell.sf.net create


# tar cf - Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar Osm2ShareNav-latest.jar Osm2ShareNav-debug-latest.jar | ssh $user,sharenav@shell.sf.net 'cd /home/project-web/sharenav/htdocs/prebuild ; tar xpf -'
#scp -p Osm2ShareNav-$ver.jar Osm2ShareNav-$ver-debug.jar $user,sharenav@web.sf.net:htdocs/prebuild
