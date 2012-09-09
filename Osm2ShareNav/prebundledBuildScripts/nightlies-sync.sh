#!/bin/sh

cd /home/kai/workspace/GIT-ShareNav/ShareNav
ant clean
ant logging j2mepolish
cd ..
cd Osm2ShareNav
ant clean
ant deploy
cd ..
scp Osm2ShareNav/dist/Osm2ShareNav-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2ShareNav-APM.jar

cd /home/kai/workspace/checkout/ShareNav
cvs update -d
date > nightlies_builtat.txt
ant clean
ant logging j2mepolish
cd ..
cd Osm2ShareNav
cvs update -d
ant clean
ant deploy
cd ..
scp Osm2ShareNav/dist/Osm2ShareNav-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2ShareNav-CVS-debug.jar

cd /home/kai/workspace/checkout/ShareNav
cvs update -d
ant clean
ant j2mepolish
cd ..
cd Osm2ShareNav
cvs update -d
ant clean
ant deploy
cd ..
scp Osm2ShareNav/dist/Osm2ShareNav-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/Osm2ShareNav-CVS.jar
scp Osm2ShareNav/high-style.zip gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/high-style.zip
scp ShareNav/nightlies_builtat.txt gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/nightlies_builtat.txt



