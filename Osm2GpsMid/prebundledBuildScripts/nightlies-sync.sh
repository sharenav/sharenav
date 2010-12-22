#!/bin/sh

cd /home/kai/workspace/GIT-GpsMid/GpsMid
ant clean
ant logging j2mepolish
cd ..
cd Osm2GpsMid
ant clean
ant deploy
cd ..
scp Osm2GpsMid/dist/Osm2GpsMid-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2GpsMid-APM.jar

cd /home/kai/workspace/checkout/GpsMid
cvs update -d
date > nightlies_builtat.txt
ant clean
ant logging j2mepolish
cd ..
cd Osm2GpsMid
cvs update -d
ant clean
ant deploy
cd ..
scp Osm2GpsMid/dist/Osm2GpsMid-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2GpsMid-CVS-debug.jar

cd /home/kai/workspace/checkout/GpsMid
cvs update -d
ant clean
ant j2mepolish
cd ..
cd Osm2GpsMid
cvs update -d
ant clean
ant deploy
cd ..
scp Osm2GpsMid/dist/Osm2GpsMid-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2GpsMid-CVS.jar
scp Osm2GpsMid/high-style.zip gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/high-style.zip
scp GpsMid/nightlies_builtat.txt gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/nightlies_builtat.txt



