#
# process /*i:Stringname*/-marked strings to Locale.get() calls
# using a messages-en.txt file, modify .java files mentioned
# in the messages-en.txt file
# 
# run with: awk -f replace-with-name.awk < messages.txt
# or 
# grep guiosmadd ../../../../../resources/messages.txt | awk -f replace-with-name.awk for a single file

# in the directory where the file is
#
# todo: find java files in different directories
# todo: check for errors in input files (missing file name etc.)
# requirements: awk,bourne-compatible shell,ls,grep,sort,sed
# author: jkpj
#
# todo: will probably choke on * inside the string, fixme
#


{
  line = $0;
  split(line, a, ".");  
  filebase=a[1];
  rest=a[2];
  split(rest, b, "=");  
  strname=b[1];
  split(line, c, "=");
  str=c[2];

  command = "ls | grep -i \"^\"" filebase ".java$";
  command | getline filename

  # escape /
  #stringtochange = "\"" str "\"/\\*i\\*/";
  #stringtochange = "\"" str "\"/*i:[^*]**/";
    stringtochange = "\"" str "\"";
  gsub(/%/,"\\%", stringtochange);
  gsub(/\(/,"(", stringtochange);
  gsub(/\)/,")", stringtochange);
  gsub(/'/,"\'\"'\"'", stringtochange);
  gsub(/\*/,"\\*", stringtochange);
  stringtochange = stringtochange  " */\\*[ ]*i:[^*]*[ *]\\*/";
  #gsub(/\'/,"'", stringtochange);
  # gsub(/\*/,"\\*", stringtochange);
  newstring = "Locale.get(\"" filebase "." strname "\")/*" str "*/";
  command = "sed 's%" stringtochange "%" newstring "%g'" ; 

  system(command "< " filename " > " filename ".tmp");
  system("cat > " filename " < " filename ".tmp");

  print "Filebase: " filebase;
  print "Filename: " filename;
  print "Strname: " strname;
  print "Str: " str;
  print "Sed command:" command;
}
