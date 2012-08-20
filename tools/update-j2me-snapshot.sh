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

ant 

#scp -p dist/*.jar $user,gpsmid@web.sf.net:htdocs/prebuild


ssh $user,gpsmid@shell.sf.net create

cd dist

ln -f -s `ls -t *full-connected*jar|head -1` GpsMid-latest.jar

tar cf - GpsMid-Generic*.jar | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'

# debug build

cd ..

ant clean

# workaround for the first i18n-messages failure
#
ant -Ddevice=Generic/blackberry


# normal build 

ant  debug

cd dist

for i in *.jar
do
 mv $i `echo $i | sed 's/Generic-/Generic-debug-/'`
done

ln -f -s `ls -t *full-connected*jar|head -1` GpsMid-latest-debug.jar

tar cf - *.jar | ssh $user,gpsmid@shell.sf.net 'cd /home/project-web/gpsmid/htdocs/prebuild ; tar xpf -'
