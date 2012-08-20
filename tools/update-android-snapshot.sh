#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=SOURCEFORGE_USERNAME_HERE
. ./android.properties

ant clean

ant -propertyfile android.properties android

# normal build 

# 

#scp -p dist/*.apk $user,gpsmid@web.sf.net:htdocs/prebuild


ssh $user,gpsmid@shell.sf.net create

cd dist

ln -f -s `ls -t *droid*full-connected-outlin*apk|head -1` GpsMid-latest.apk

tar cf - *.apk | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'

# debug build

cd ..

ant clean

ant -propertyfile android.properties debug android

cd dist

for i in *.apk
do
 mv $i `echo $i | sed 's/Generic-/Generic-debug-/'`
done

ln -f -s `ls -t *droid*apk|head -1` GpsMid-latest-debug.apk

tar cf - *.apk | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'
