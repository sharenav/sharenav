#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=PUT_YOUR_SOURCEFORGE_USERNAME_HERE

ant clean

ant -propertyfile android.properties android

# normal build 

# 

scp -p dist/*.apk $user,gpsmid@web.sf.net:htdocs/prebuild
