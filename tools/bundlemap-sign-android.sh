#!/bin/sh

# tested on Ubuntu 10.10

# obsolete since 2012-07-29: integrated into bundlemap.sh with a new method
# for bundling

if [ -r android.properties ]
then

    androidhome=`grep "^android.home=" < android.properties | head -1 | cut -d= -f 2`

    if [ ! "$androidhome" ]
    then
	echo "$0: error: android.home not found in android.properties"
	exit 1
    fi
    # echo "Android home: $androidhome"

    midtarget="$1"
    # assumes version is x.y.z-mapxx
    ver=`echo $midtarget | sed 's/\.ap_//g' | tail -12c`
    # assumes version is x.y.zz-mapxx
    ver=`echo $midtarget | sed 's/\.ap_//g' | tail -13c`
    # FIXME assumes version is x.y-mapxx when it could be x.y.z-mapxx
    #ver=`echo $midtarget | sed 's/\.ap_//g' | tail -10c`
    #echo "ver: $ver"
    midapk=`echo $midtarget|sed 's/\.ap_/.apk/'`
    andtarget=`echo $midtarget|sed 's/ShareNav-Generic-//g'`
    andtarget=`echo $andtarget|sed "s/-$ver.ap_//g"`
    echo $andtarget

    $androidhome/platforms/android-8/tools/aapt package -f -M build/real/Generic/$andtarget/en/activity/AndroidManifest.xml -S build/real/Generic/$andtarget/en/activity/res -A assets -0 wav -0 amr -0 mp3 -0 ogg -0 png -I $androidhome/platforms/android-8/android.jar -F dist/$midtarget 

    java -Xmx128M -Djava.ext.dirs=$androidhome/tools/lib -Djava.library.path=$androidhome/tools/lib -jar $androidhome/tools/lib/apkbuilder.jar dist/$midapk -z dist/$midtarget -f build/real/Generic/$andtarget/en/activity/bin/classes.dex -rj build/real/Generic/$andtarget/en/activity/libs

else
    echo "$0: android.properties doesn't exist; create one from android.properties.default and try again"
    exit 2
fi
