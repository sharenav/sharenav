#!/bin/bash

ulimit -Sv 14000000
renice 15 $$

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

skip=0;
idx=1;
idx=$[$idx+1];
skip=$[$skip+1];

echo $idx
echo $skip

cd /home/apmon/Osm2GpsMid


if [ $skip -lt $idx ]
then
java -mx800M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar  --cellID=cells.txt.gz /home/apmon/planet_dumps/malta.osm.pbf Malta
scp GpsMidMt-0.7.11-map65.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=trinidad
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Trinidad
scp GpsMidTt-0.7.11-map65.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=uae
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm UAE
scp GpsMidUAE-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

exit

if [ $skip -lt $idx ]
then
c=afghanistan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Afghanistan
scp GpsMidAf-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=jamaica
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Jamaica
scp GpsMidJm-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=bangladesh
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bangladesh
scp GpsMidBd-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=mexico
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Mexico
scp GpsMidMx-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=venezuela
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Venezuela
scp GpsMidVe-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=lebanon
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Lebanon
scp GpsMidLb-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=singapore
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Singapore
scp GpsMidSg-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=jordan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Jordan
scp GpsMidJo-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=syria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Syria
scp GpsMidSy-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=bahrain
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bahrain
scp GpsMidBh-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=brunei
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Brunai
scp GpsMidBn-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar  --cellID=cells.txt.gz /home/apmon/planet_dumps/colombia.osm.pbf Colombia
scp GpsMidCo-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]


if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar  --cellID=cells.txt.gz /home/apmon/planet_dumps/argentina.osm.pbf Argentina
scp GpsMidAr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar  --cellID=cells.txt.gz /home/apmon/planet_dumps/iraq.osm.pbf Iraq
scp GpsMidIq-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/kazakhstan.osm.pbf Kazakhstan
scp GpsMidKz-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/canary.osm.pbf Canary
scp GpsMidCanary-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar  --cellID=cells.txt.gz /home/apmon/planet_dumps/czech_republic.osm.pbf Czech
scp GpsMidCz-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/spain.osm.pbf Madrid
scp GpsMidMad-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx2700M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/haiti.osm.pbf Haiti
scp GpsMidHt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=indonesia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Indonesia
scp GpsMidId-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]


if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf CentralAustralia
scp GpsMidCA-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf WesternAustralia
scp GpsMidWA-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=sri_lanka
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Sri_Lanka
scp GpsMidLk-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf Queensland
scp GpsMidQu-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf Victoria
scp GpsMidVic-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf NSW
scp GpsMidNSW-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Stuttgart
scp GpsMidSt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
%c=louisiana
%rm /tmp/$c.osm
%mkfifo /tmp/$c.osm
%bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NewOrleans
%scp GpsMidNO-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=colorado
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Colorado
scp GpsMidCol-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=new_york
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NYC
scp GpsMidNYC-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=puerto_rico
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar /tmp/$c.osm PuertoRico
scp GpsMidPr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Schleswig
scp GpsMidSW-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=california
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm BayArea
scp GpsMidBayArea-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=california
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm LosAngeles
scp GpsMidLA-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]


if [ $skip -lt $idx ]
then
c=britishcolumbia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm BritishColumbia
scp GpsMidBC-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=ontario
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ontario
scp GpsMidON-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/poland.osm.pbf Poland
scp GpsMidPl-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/poland.osm.pbf Warsaw
scp GpsMidWarsaw-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/poland.osm.pbf Krakow
scp GpsMidKrakow-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/poland.osm.pbf Lodz
scp GpsMidLodz-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/australia-oceania.osm.pbf Australia
scp GpsMidAu-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
java -mx5300M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/sweden.osm.pbf Sweden
scp GpsMidSe-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

if [ $skip -lt $idx ]
then
c=japan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx10000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Japan
scp GpsMidJp-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
fi
idx=$[$idx+1]

c=belarus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Belarus
scp GpsMidBy-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=albania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Albania
scp GpsMidAl-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &


c=croatia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Croatia
scp GpsMidHR-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=bosnia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bosnia-Herzegovina
scp GpsMidBa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=bulgaria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bulgaria
scp GpsMidBg-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=estonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Estonia
scp GpsMidEE-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=kosovo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kosovo
scp GpsMidKos-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=lithuania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Lithuania
scp GpsMidLt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=latvia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Latvia
scp GpsMidLv-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=luxembourg
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Luxembourg
scp GpsMidLu-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=macedonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Macedonia
scp GpsMidMk-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=moldova
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Moldova
scp GpsMidMd-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/slovakia.osm.pbf Slovakia
scp GpsMidSk-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=slovenia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Slovenia
scp GpsMidSl-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Finland
scp GpsMidFi-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Helsinki
scp GpsMidHel-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

%java -mx12700M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/netherlands.osm.pbf Netherlands
%scp GpsMidNl-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5300M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/netherlands.osm.pbf Amsterdam
scp GpsMidAmst-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf Rome
scp GpsMidRome-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf Milano
scp GpsMidMilano-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf Torino
scp GpsMidTorino-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx6200M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf SouthItaly
scp GpsMidSIt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx7200M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf NorthItaly
scp GpsMidNIt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx6200M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/italy.osm.pbf CentralItaly
scp GpsMidCIt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=ethiopia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ethiopia
scp GpsMidEt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=nigeria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Nigeria
scp GpsMidNe-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=egypt
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Egypt
scp GpsMidEg-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=morocco
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Morocco
scp GpsMidMa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=congo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Congo
scp GpsMidCd-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=kenya
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kenya
scp GpsMidKe-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=seychelles
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Seychelles
scp GpsMidSc-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=namibia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Namibia
scp GpsMidNa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=tunesia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Tunesia
scp GpsMidTn-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/brazil.osm.pbf Brazil
scp GpsMidBr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=new_zealand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NewZealand
scp GpsMidNZ-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/china.osm.pbf China
scp GpsMidCn-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=malaysia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Malaysia
scp GpsMidMy-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=thailand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Thailand
scp GpsMidTh-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=saudi_arabia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Saudi
scp GpsMidSa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx2000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/south_africa.osm.pbf South_Africa
scp GpsMidZa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=russia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm moscow
scp GpsMidMos-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=russia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm StPetersburg
scp GpsMidStPeter-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=denmark
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx4000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Denmark
scp GpsMidDk-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=zimbabwe
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Zimbabwe
scp GpsMidZw-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=south_korea
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm South_Korea
scp GpsMidKr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/pakistan.osm.pbf Pakistan
scp GpsMidPk-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/india.osm.pbf India
scp GpsMidIn-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/india.osm.pbf SouthIndia
scp GpsMidSIn-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/israel.osm.pbf Israel
scp GpsMidIl-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=iran
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iran
scp GpsMidIr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=ukraine
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ukraine
scp GpsMidUa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=turkey
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Turkey
scp GpsMidTr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=ireland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ireland
scp GpsMidIe-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=hungary
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Hungary
scp GpsMidHu-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=cyprus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Cyprus
scp GpsMidCy-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

c=greece
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Greece
scp GpsMidGr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &


c=iceland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iceland
scp GpsMidIs-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/romania.osm.pbf Romania
scp GpsMidRo-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &


java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/great_britain.osm.pbf South-Scotland
scp GpsMidScot-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/great_britain.osm.pbf london
scp GpsLon-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/great_britain.osm.pbf SouthWestEngland
scp GpsMidSWE-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/great_britain.osm.pbf NorthEngland
scp GpsNEng-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz  /home/apmon/planet_dumps/germany.osm.pbf RheinMain
scp GpsMidRM-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf nuernberg
scp GpsMidN-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Cologne
scp GpsMidKB-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Berlin
scp GpsMidB-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Munich
scp GpsMun-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf hamburg
scp GpsMidHH-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx12500M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Bayern
scp GpsMidBay-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf Karlsruhe
scp GpsMidKa-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/germany.osm.pbf RuhrGebiet
scp GpsMidRuhr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/austria.osm.pbf Wien
scp GpsMidW-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/austria.osm.pbf Austria
scp GpsMidAt-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/france.osm.pbf Paris
scp GpsMidP-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
java -mx5600M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/netherlands.osm.pbf AmsterdamRotterdam
scp GpsMidAR-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/portugal.osm.bz2 > /tmp/portugal.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/portugal.osm Portugal
scp GpsMidPort-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &
bzcat  /home/apmon/planet_dumps/norway.osm.bz2 > /tmp/norway.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/norway.osm SouthNorway
scp GpsMidSNor-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/czech_republic.osm.pbf Praha
scp GpsMidPr-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/spain.osm.pbf Spain
scp GpsMidSpain-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx5000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/switzerland.osm.pbf Switzerland
scp GpsMidSwiss-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

bzcat  /home/apmon/planet_dumps/belgium.osm.bz2 > /tmp/belgium.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/belgium.osm Belgium
scp GpsMidBelg-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &

java -mx3000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/philippines.osm.pbf Philippines
scp GpsMidPh-0.7.11-map65.ja* gpsmidW:/home/project-web/gpsmid/htdocs/prebuild/ &


java -mx4000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar /home/apmon/planet_dumps/germany.osm.pbf Munic-demo
scp GpsMidDemo-0.7.11-map65.jar gpsmidW:/home/project-web/gpsmid/htdocs/GpsMunicDemo-munic-demo-CVS.jar &

java -mx4000M -XX:NewRatio=32 -jar Osm2GpsMid-0.7.11-map65.jar /home/apmon/planet_dumps/germany.osm.pbf Munic-demo
scp GpsMidDemo-0.7.11-map65.jar gpsmidW:/home/project-web/gpsmid/htdocs/GpsMunicDemo-munic-demo-0.7.1.jar &

#bzcat  /home/apmon/planet_dumps/germany.osm.bz2 > /tmp/germany.osm & java -mx4000M -jar Osm2GpsMid-0.6.jar /tmp/germany.osm Munic-demo
#scp GpsMidDemo-0.6.jar gpsmidW:/home/project-web/gpsmid/htdocs/ &

