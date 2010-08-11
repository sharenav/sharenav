#!/bin/sh

# grab GpsMid i18n files from the wiki, write to messages_xx.txt
# requirements: awk,bourne-compatible shell,ls,grep,sort,sed
#

# define languages to fetch
files="$@"

cat $files | awk -f replace.awk
