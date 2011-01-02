#!/bin/sh
  
# read in a gettext .po file, output a messages_la.txt J2MEPolish file

awk ' BEGIN { inentry = 0 }
      /^#: dummy.java / { inentry = 1 }
      /^msgid / {     key = substr ($0, 8, index($0, "=") - 8)
      valen = substr ($0, index($0, "=") + 1) }
      /^msgstr/ { if ($0 != "msgstr \"\"") {
              value = substr ($0, index($0, "\"") + 1)
              value = substr(value, 0, length(value)-1);
              print key "=" value;
}}'
