#!/bin/sh
#
# Sign a Midlet
#

if [ $# -ne 3 ]
then
	echo "Usage: signmidlet.sh passphrase keystore-file midlet-name (without .jad/.jar)"
	exit
fi

java -jar /usr/local/WTK2.5.2/bin/JadTool.jar -addjarsig -keypass $1 -alias fossgis-key -keystore $2 -inputjad $3.jad -outputjad $3.jad -jarfile $3.jar

java -jar /usr/local/WTK2.5.2/bin/JadTool.jar -addcert -alias fossgis-key -keystore $2 -inputjad $3.jad -outputjad $3.jad
