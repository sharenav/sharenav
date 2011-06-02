#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=PUT_YOUR_SOURCEFORGE_USERNAME_HERE
ver=0.7.52-map66

ant clean

ant -propertyfile android.properties android

# normal build 

# 

scp -p dist/*.apk $user,gpsmid@web.sf.net:htdocs/prebuild
