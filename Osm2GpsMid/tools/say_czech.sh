#!/bin/bash
#say-epos --voice "machac" -z -w `echo "$2" | iconv -t iso8859-2 -`
say-epos --voice "machac" -w `echo "$2" | iconv -t iso8859-2 -`
cp said.wav $1
rm said.wav
