#!/bin/sh
#
# update the GpsMid "bare binaries" J2ME snapshots
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

#scp -p dist/*.jar $user,gpsmid@web.sf.net:htdocs/prebuild


ssh $user,gpsmid@shell.sf.net create

cd dist

ln -f -s `ls -t *full-connected*jar|head -1` GpsMid-latest.jar

cd ..

for i in full-connected full midisize minimal blackberry
do
    ./tools/bundlemap.sh minimap $i
done

tar cf - GpsMid-Generic*.jar GpsMid-latest.jar | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'

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

for i in full-connected full midisize minimal blackberry
do
    ./tools/bundlemap.sh minimap debug-$i
done


ln -f -s `ls -t *full-connected*jar|head -1` GpsMid-latest-debug.jar

tar cf - GpsMid-Generic-debug-*.jar GpsMid-latest-debug.jar | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'
