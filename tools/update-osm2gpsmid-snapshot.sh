#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=SOURCEFORGE_USERNAME_HERE
ver=0.7.65-map69

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

ln -f -s Osm2GpsMid-$ver-debug.jar Osm2GpsMid-debug-latest.jar
ln -f -s Osm2GpsMid-$ver.jar Osm2GpsMid-latest.jar

# 

ssh $user,gpsmid@shell.sf.net create

tar cf - Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar Osm2GpsMid-latest.jar Osm2GpsMid-debug-latest.jar | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'
#scp -p Osm2GpsMid-$ver.jar Osm2GpsMid-$ver-debug.jar $user,gpsmid@web.sf.net:htdocs/prebuild
