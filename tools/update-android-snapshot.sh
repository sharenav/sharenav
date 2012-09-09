#!/bin/sh
#
# update the Android ShareNav "bare binaries" snapshots
# FIXME: bundle a small map with the app

user=SOURCEFORGE_USERNAME_HERE
. ./android.properties

ant clean

ant -propertyfile android.properties android

# normal build 

# 

#scp -p dist/*.apk $user,sharenav@web.sf.net:htdocs/prebuild

# FIXME update upload location

# ssh $user,sharenav@shell.sf.net create

cd dist


cd ..

ln -f -s `ls -t *droid*full-connected*apk|head -1` ShareNav-latest.apk

for i in full-connected full midsize minimal
do
    ./tools/bundlemap.sh -a assets android-$i
done


# FIXME update upload location
# tar cf - *.apk | ssh $user,sharenav@shell.sf.net 'cd /home/project-web/sharenav/htdocs/prebuild ; tar xpf -'

# debug build

ant clean

ant -propertyfile android.properties debug android

cd dist

for i in *.apk
do
 mv $i `echo $i | sed 's/Generic-android/Generic-android-debug/'`
done

cd ..

for i in full-connected full midsize minimal
do
    ./tools/bundlemap.sh -a assets android-debug-$i
done

ln -f -s `ls -t *droid*apk|head -1` ShareNav-latest-debug.apk

# FIXME update upload location
# tar cf - *.apk | ssh $user,sharenav@shell.sf.net 'cd /home/project-web/sharenav/htdocs/prebuild ; tar xpf -'
