#!/bin/bash

if [ "$2" == "" ] ; then
    echo "usage: wavtoamr wavfile amrfile"
    exit 1
fi

midwav="$(mktemp)"
midamr="$(mktemp)"
sox "$1" -r 8000 -c 1 "$midwav.wav" &&
gst-launch-0.10 filesrc location="$midwav.wav" ! wavparse ! amrnbenc ! filesink location="$midamr" &&
echo '#!AMR' > "$2" &&
cat "$midamr" >> "$2"
rm -f "$midwav.wav"
rm -f "$midamr"
