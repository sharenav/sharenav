#!/bin/sh
#
# update the Osm2GpsMid snapshots
#

user=jkpj

ant clean

ant -propertyfile android.properties android

# normal build 

# 

scp -p dist/*.apk $user,gpsmid@web.sf.net:htdocs/prebuild
