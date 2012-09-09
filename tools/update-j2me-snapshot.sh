#!/bin/sh
#
# update the ShareNav "bare binaries" J2ME snapshots
# FIXME: bundle a small map with the app
#

user=SOURCEFORGE_USERNAME_HERE
. ./android.properties

ant clean

# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry


# normal build 

ant j2mepolish

#scp -p dist/*.jar $user,sharenav@web.sf.net:htdocs/prebuild


# FIXME update location
#ssh $user,sharenav@shell.sf.net create

cd dist


cd ..

ln -f -s `ls -t *full-connected*jar|head -1` ShareNav-latest.jar

for i in full-connected full midsize minimal blackberry
do
    ./tools/bundlemap.sh minimap $i
done

# FIXME update location
# tar cf - ShareNav-Generic*.jar ShareNav-latest.jar | ssh $user,sharenav@shell.sf.net 'cd /home/project-web/sharenav/htdocs/prebuild ; tar xpf -'

# debug build

# cd ..

ant clean

# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry


# normal build 

ant  debug j2mepolish

cd dist

for i in *.jar
do
 mv $i `echo $i | sed 's/Generic-/Generic-debug-/'`
done

cd ..

for i in full-connected full midsize minimal blackberry
do
    ./tools/bundlemap.sh minimap debug-$i
done


ln -f -s `ls -t *full-connected*jar|head -1` ShareNav-latest-debug.jar

# FIXME update location
# tar cf - ShareNav-Generic-debug-*.jar ShareNav-latest-debug.jar | ssh $user,sharenav@shell.sf.net 'cd /home/project-web/sharenav/htdocs/prebuild ; tar xpf -'
