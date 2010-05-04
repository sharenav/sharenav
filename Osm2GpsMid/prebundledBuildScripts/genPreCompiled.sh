#!/bin/sh

ulimit -Sv 8000000

rm /tmp/great_b.osm
mkfifo /tmp/great_b.osm
rm /tmp/germany.osm
mkfifo /tmp/germany.osm
rm /tmp/austria.osm
mkfifo /tmp/austria.osm
rm /tmp/france.osm
mkfifo /tmp/france.osm
rm /tmp/netherlands.osm
mkfifo /tmp/netherlands.osm
rm /tmp/norway.osm
mkfifo /tmp/norway.osm
rm /tmp/portugal.osm
mkfifo /tmp/portugal.osm
rm /tmp/czech.osm
mkfifo /tmp/czech.osm
rm /tmp/spain.osm
mkfifo /tmp/spain.osm
rm /tmp/switzerland.osm
mkfifo /tmp/switzerland.osm
rm /tmp/belgium.osm
mkfifo /tmp/belgium.osm
rm /tmp/philippines.osm
mkfifo /tmp/philippines.osm
rm /tmp/finland.osm
mkfifo /tmp/finland.osm
rm /tmp/italy.osm
mkfifo /tmp/italy.osm


cd /home/apmon/Osm2GpsMid

c=trinidad
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Trinidad
scp GpsMidTt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=haiti
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Haiti
scp GpsMidHt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=indonesia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Indonesia
scp GpsMidId-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &



c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm CentralAustralia
scp GpsMidCA-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm WesternAustralia
scp GpsMidWA-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=sri_lanka
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Sri_Lanka
scp GpsMidLk-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Queensland
scp GpsMidQu-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Victoria
scp GpsMidVic-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NSW
scp GpsMidNSW-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=germany
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Stuttgart
scp GpsMidSt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=louisiana
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NewOrleans
scp GpsMidNO-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=puerto_rico
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar /tmp/$c.osm PuertoRico
scp GpsMidPr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=germany
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Schleswig
scp GpsMidSW-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=california
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm BayArea
scp GpsMidBayArea-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=california
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm LosAngeles
scp GpsMidLA-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &



c=britishcolumbia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm BritishColumbia
scp GpsMidBC-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ontario
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ontario
scp GpsMidON-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=poland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Poland
scp GpsMidPl-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Australia
scp GpsMidAu-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


c=sweden
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Sweden
scp GpsMidSe-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=japan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Japan
scp GpsMidJp-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=belarus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Belarus
scp GpsMidBy-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=albania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Albania
scp GpsMidAl-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


c=croatia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Croatia
scp GpsMidHR-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=bosnia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bosnia-Herzegovina
scp GpsMidBa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=bulgaria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bulgaria
scp GpsMidBg-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=estonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Estonia
scp GpsMidEE-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=kosovo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kosovo
scp GpsMidKos-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=lithuania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Lithuania
scp GpsMidLt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=latvia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Latvia
scp GpsMidLv-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=luxembourg
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Luxembourg
scp GpsMidLu-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=macedonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Macedonia
scp GpsMidMk-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=moldova
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Moldova
scp GpsMidMd-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=slovakia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Slovakia
scp GpsMidSk-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=slovenia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Slovenia
scp GpsMidSl-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Finland
scp GpsMidFi-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Helsinki
scp GpsMidHel-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=netherlands
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx8700M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Netherlands
scp GpsMidNl-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=netherlands
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5300M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Amsterdam
scp GpsMidAmst-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Rome
scp GpsMidRome-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx6200M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm SouthItaly
scp GpsMidSIt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx7200M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NorthItaly
scp GpsMidNIt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx6200M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm CentralItaly
scp GpsMidCIt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ethiopia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ethiopia
scp GpsMidEt-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=nigeria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Nigeria
scp GpsMidNe-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=egypt
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Egypt
scp GpsMidEg-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=morocco
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Morocco
scp GpsMidMa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=congo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Congo
scp GpsMidCd-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=kenya
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kenya
scp GpsMidKe-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=seychelles
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Seychelles
scp GpsMidSc-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=namibia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Namibia
scp GpsMidNa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=tunesia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Tunesia
scp GpsMidTn-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=brazil
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Brazil
scp GpsMidBr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=new_zealand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NewZealand
scp GpsMidNZ-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=china
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm China
scp GpsMidCn-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=malaysia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Malaysia
scp GpsMidMy-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=thailand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Thailand
scp GpsMidTh-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=saudi_arabia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Saudi
scp GpsMidSa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=south_africa
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx2000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm South_Africa
scp GpsMidZa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=russia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm moscow
scp GpsMidMos-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=denmark
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx4000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Denmark
scp GpsMidDk-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=zimbabwe
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Zimbabwe
scp GpsMidZw-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=south_korea
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm South_Korea
scp GpsMidKr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=pakistan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Pakistan
scp GpsMidPk-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=india
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm India
scp GpsMidIn-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=israel
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Israel
scp GpsMidIl-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &



c=iran
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iran
scp GpsMidIr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ukraine
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ukraine
scp GpsMidUa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=turkey
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Turkey
scp GpsMidTr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ireland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ireland
scp GpsMidIe-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=hungary
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Hungary
scp GpsMidHu-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=cyprus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Cyprus
scp GpsMidCy-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=greece
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Greece
scp GpsMidGr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


c=iceland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iceland
scp GpsMidIs-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=romania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Romania
scp GpsMidRo-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


bzcat /home/apmon/planet_dumps/great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm South-Scotland
scp GpsMidScot-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm london
scp GpsLon-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm SouthWestEngland
scp GpsMidSWE-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm NorthEngland
scp GpsNEng-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & time java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm RheinMain
scp GpsMidRM-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & time java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm nuernberg
scp GpsMidN-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Cologne
scp GpsMidKB-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Berlin
scp GpsMidB-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Munich
scp GpsMun-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm hamburg
scp GpsMidHH-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx5500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Bayern
scp GpsMidBay-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Karlsruhe
scp GpsMidKa-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm RuhrGebiet
scp GpsMidRuhr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/austria.osm.bz2 > /tmp/austria.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/austria.osm Wien
scp GpsMidW-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/france.osm.bz2 > /tmp/france.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/france.osm Paris
scp GpsMidP-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/netherlands.osm.bz2 > /tmp/netherlands.osm & java -mx3600M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/netherlands.osm AmsterdamRotterdam
scp GpsMidAR-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/portugal.osm.bz2 > /tmp/portugal.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/portugal.osm Portugal
scp GpsMidPort-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/norway.osm.bz2 > /tmp/norway.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/norway.osm SouthNorway
scp GpsMidSNor-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/czech_republic.osm.bz2 > /tmp/czech.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/czech.osm Praha
scp GpsMidPr-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/spain.osm.bz2 > /tmp/spain.osm & java -mx3500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/spain.osm Spain
scp GpsMidSpain-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/switzerland.osm.bz2 > /tmp/switzerland.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/switzerland.osm Switzerland
scp GpsMidSwiss-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/belgium.osm.bz2 > /tmp/belgium.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/belgium.osm Belgium
scp GpsMidBelg-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/philippines.osm.bz2 > /tmp/philippines.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/philippines.osm Philippines
scp GpsMidPh-0.6.2.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx4000M -jar Osm2GpsMid-CVS.jar /tmp/germany.osm Munic-demo
scp GpsMidDemo-Munic-demo-0.6.2.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/GpsMunicDemo-munic-demo-CVS.jar &

bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx4000M -jar Osm2GpsMid-0.6.jar /tmp/germany.osm Munic-demo
scp GpsMidDemo-0.6.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/ &

