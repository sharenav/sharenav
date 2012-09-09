#!/bin/sh

# Create voice files for different languages with festival
# input files: voice-english.txt, voice-finnish.txt, voice-italian.txt etc.
# where the language part is a language support by festival with festival --language languagename
# output files: prepare.wav, prepare-fi.wav  etc.

filesuffix=".wav"

for langfile in voice-*.txt
do
  lang=`echo $langfile|sed 's/^voice-//' | sed 's/\.txt$//'`
  while read textline
  do
    filebase=`echo $textline|cut -d: -f1`
    text=`echo $textline|cut -d: -f2`
    langshort=`echo $lang|head -2c`
    if [ "$lang" = "english" ]
    then
      langarg=""
    else
      langarg="--language $lang"
    fi
#    if [ "$lang" = "english" ]
#    then
#      filesuffix=".wav"
#    else
#      filesuffix="-$langshort.wav"
#    fi
    if [ "$text" ]
    then
      echo "text: $text"
      echo "(utt.save.wave (SynthText \"$text\") \"$filebase$filesuffix\" \"wav\")" | festival $langarg
    else
      cp -p empty.wav $filebase$filesuffix
    fi
  done < "$langfile"
done

# create .wav files with festival

# resample commented out for now so we can test with microemulator

for file in *.wav
do
##  #filebase=`basename $file`
#  # fix wav file
  sox "$file" repair.wav
  # resample it to 8kHz with 16 bits and single channel, increase volume by 50 %
  sox repair.wav -b 16 -c 1 -r 8k "$file" vol 1.50
  # increase volume, resample to 16000 for microemulator
#  sox repair.wav -b 16 -r 16k "$file" vol 1.50
  rm repair.wav
done

# convert the wav files to mp3

for file in *.wav
do
  lame $file `basename $file .wav`.mp3
done



# Finally convert the wav-files to AMR format
# ShareNav's standard sound files are at 4.75kbit/s which is the lowest possible bit rate
# don't have the command for this available, apparently I'm missing the amr codec
# this uses MR122 for a better sound quality
#for file in *.wav
#do
#  code-for-amr-program-or-sox-with-parametres  $file `basename $file .wav`.amr
#done
#
#package is: amrnb
#

for file in *.wav
do
  amrnb-encoder MR122 $file `basename $file .wav`.amr
done
