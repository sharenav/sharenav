#!/bin/sh
  
# grab ShareNav i18n (language) messages from the wiki, write to messages_lang.txt
# requirements: lynx,awk,bourne-compatible shell 
# usage: $0 [ lang1 lang2 lang3 ... ]
  
# FIXME update location

echo "Don't know how to grab messages at the moment"

exit 2

# default languages to fetch
lang="cs en de es fi fr it pl ru sk" 

if [ "$#" -gt 0 ]
then
  lang="$@"
fi

path=resources
  
for l in $lang
do
   lynx -source http://sourceforge.net/apps/mediawiki/sharenav/index.php?title=I18n/messages_$l |
   awk 'BEGIN { pr = 0 } 
     /<\/pre>/ { pr = 0 } 
     /=section=/ { pr = 0 } 
     { if (pr) {
   if (substr ($0, 1, 1) == " ") { print substr ($0, 2) } else { if ($0 != "" && substr($0, 0, 1) != "#") { print; } } } }
     /section=messages;/ { pr = 1 } 
     /<pre>/ { pr = 1 } 
     ' | sed 's/&amp;/\&/g'| sed 's/&lt;/\</g' | sed 's/&gt;/\>/g' | sed 's/&nbsp;/ /g' | sed 's/&quot;/"/g' > $path/messages_$l.txt
done
mv $path/messages_en.txt $path/messages.txt
