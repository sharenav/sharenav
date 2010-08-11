#!/bin/sh

# grab GpsMid strings to i18nize from .java sources, write to stdout in format suitable for messages.txt
# requirements: grep,awk,bourne-compatible shell,sort,uniq
# /dev/null is an empty file used to make grep output filenames even if there's only one file
#

# define languages to fetch
files="$@"

grep "/\* i:.*\*/" /dev/null $files |
awk -F: '{
  file = $1;
  filebase = tolower(substr(file, 0, index($1, ".java") - 1));
  line = $0;
  ind = index (line, "/* i:");
  matchend = index (line, "*/");
  strname=substr (line, ind + 5, matchend - ind - 6);
  do {
   last = split(substr(line, 0, ind), a, "\"");
   str = "." strname "=" a[last-1];
   print filebase str;
   line = substr(line, matchend+2);
   ind = index (line, "/* i:");
   matchend = index (line, "*/");
   strname=substr (line, ind + 5, matchend - ind - 5);
  } while(ind > 0);
}' |sort|uniq

