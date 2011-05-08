#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=PUT_YOUR_SOURCEFORGE_USERNAME_HERE
ver=0.7.31-map65

ant clean
#
# workaround for the first i18n-messages failure
#
ant -propertyfile android.properties -Ddevice=Generic/android-hiresonline
ant -propertyfile android.properties -Ddevice=Generic/android

# normal build 

# 

scp -p dist/*.apk $user,gpsmid@web.sf.net:htdocs/prebuild
