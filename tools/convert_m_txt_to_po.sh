#!/bin/sh
  
# read in a messages_la.txt J2MEPolish file
# output a a gettext .po file
#

awk ' BEGIN { while ((getline < "resources/messages.txt") > 0 ) {
    key = substr ($0, 0, index($0, "=") - 1)
    #print key
    en[key] = substr ($0, index($0, "=") + 1)
    ln[key] = i++;
    #print en[key]
    }
   lan[0] = "resources/messages_cs.txt"
   lan[1] = "resources/messages_de.txt"
   lan[2] = "resources/messages_es.txt"
   lan[3] = "resources/messages_fi.txt"
   lan[4] = "resources/messages_fr.txt"
   lan[5] = "resources/messages_it.txt"
   lan[6] = "resources/messages_pl.txt"
   lan[7] = "resources/messages_ru.txt"
   lan[8] = "resources/messages_sk.txt"
   for (i in lan) {
   print lan[i]
   delete la
    while ((getline < lan[i] ) > 0 ) {
     outfile = lan[i] ".po"
    key = substr ($0, 0, index($0, "=") - 1)
     #print key
     la[key] = substr ($0, index($0, "=") + 1)
   }
     printf "" > outfile
     print "msgid \"\"" >> outfile
     print "msgstr \"\"" >> outfile
     print "\"Project-Id-Version: ShareNav 0.8.2\\n\""  >> outfile
     print "\"MIME-Version: 1.0\\n\""  >> outfile
     print "\"Content-Type: text/plain; charset=utf-8\\n\""  >> outfile
     print "\"Content-Transfer-Encoding: 8bit\\n\""  >> outfile
     print "\"X-Poedit-SourceCharset: utf-8\\n\"" >> outfile
   for (s in en) {
     #print " en: " en[key] " " lan[i] ": " la[key] 
     print "#: dummy.java:" ln[s] >> outfile
     print "msgid \"" s "=" en[s] "\"" >> outfile
     print "msgstr \"" la[s] "\"" >> outfile
     printf "\n" >> outfile
    } 
   }
  }'
