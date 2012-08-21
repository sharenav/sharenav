#!/bin/sh
#
# update the Android GpsMid "bare binaries" snapshots
# FIXME: bundle a small map with the app

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

cd ..

for i in full-connected full midsize minimal
do
    ./tools/bundlemap.sh -a assets android-$i-outlines
done


tar cf - *.apk | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'

# debug build

ant clean

ant -propertyfile android.properties debug android

cd dist

for i in *.apk
do
 mv $i `echo $i | sed 's/Generic-android/Generic-android-debug-/'`
done

cd ..

for i in full-connected full midsize minimal
do
    ./tools/bundlemap.sh -a assets android-debug-$i-outlines
done

ln -f -s `ls -t *droid*apk|head -1` GpsMid-latest-debug.apk

tar cf - *.apk | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'
