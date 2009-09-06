#!/bin/sh

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


cd /unsafe/krueger/Osm2GpsMid

c=australia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Australia
scp GpsMidAu-Australia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=sweden
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Sweden
scp GpsMidSe-Sweden-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=japan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Japan
scp GpsMidJp-Japan-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=belarus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Belarus
scp GpsMidBy-Belarus-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=albania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Albania
scp GpsMidAl-Albania-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


c=croatia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Croatia
scp GpsMidHR-Croatia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=bosnia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bosnia-Herzegovina
scp GpsMidBa-Bosnia-Herzegovina-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=bulgaria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Bulgaria
scp GpsMidBg-Bulgaria-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=estonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Estonia
scp GpsMidEE-Estonia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=kosovo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kosovo
scp GpsMidKos-Kosovo-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=lithuania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Lithuania
scp GpsMidLt-Lithuania-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=latvia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Latvia
scp GpsMidLv-Latvia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=luxembourg
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Luxembourg
scp GpsMidLu-Luxembourg-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=macedonia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Macedonia
scp GpsMidMk-Macedonia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=moldova
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Moldova
scp GpsMidMd-Moldova-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=slovakia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Slovakia
scp GpsMidSk-Slovakia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=slovenia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Slovenia
scp GpsMidSl-Slovenia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Finland
scp GpsMidFi-Finland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=finland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Helsinki
scp GpsMidHel-Helsinki-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=netherlands
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx6700M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Netherlands
scp GpsMidNl-Netherlands-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=netherlands
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5300M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Amsterdam
scp GpsMidAmst-Amsterdam-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Rome
scp GpsMidRome-Rome-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=italy
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx5200M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NorthItaly
scp GpsMidNit-NorthItaly-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ethiopia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ethiopia
scp GpsMidEt-Ethiopia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=nigeria
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Nigeria
scp GpsMidNe-Nigeria-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=egypt
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Egypt
scp GpsMidEg-Egypt-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=morocco
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Morocco
scp GpsMidMa-Morocco-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=congo
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Congo
scp GpsMidCd-DRC-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=kenya
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Kenya
scp GpsMidKe-Kenya-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=seychelles
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Seychelles
scp GpsMidSc-Seychelles-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=namibia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Namibia
scp GpsMidNa-Namibia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=tunesia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Tunesia
scp GpsMidTn-Tunisia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=brazil
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Brazil
scp GpsMidBr-Brazil-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=new_zealand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm NewZealand
scp GpsMidNZ-NewZealand-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=china
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm China
scp GpsMidCn-China-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=malaysia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Malaysia
scp GpsMidMy-Malaysia-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=thailand
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Thailand
scp GpsMidTh-Thailand-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=saudi_arabia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Saudi
scp GpsMidSa-Saudi-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=south_africa
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx2000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm South_Africa
scp GpsMidZa-SouthAfrica-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=rusia
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm moscow
scp GpsMidMos-Moskow-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=denmark
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx4000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Denmark
scp GpsMidDk-Denmark-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=zimbabwe
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Zimbabwe
scp GpsMidZw-Zimbabwe-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=south_korea
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm South_Korea
scp GpsMidKr-SouthKorea-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=pakistan
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Pakistan
scp GpsMidPk-Pakistan-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=india
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm India
scp GpsMidIn-India-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=israel
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Israel
scp GpsMidIl-Israel-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &



c=iran
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iran
scp GpsMidIr-Iran-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ukraine
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ukraine
scp GpsMidUa-Ukraine-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=turkey
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Turkey
scp GpsMidTr-Turkey-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=ireland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Ireland
scp GpsMidIe-Ireland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=hungary
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Hungary
scp GpsMidHu-Hungary-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=cyprus
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Cyprus
scp GpsMidCy-Cyprus-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=greece
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Greece
scp GpsMidGr-Greece-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


c=iceland
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Iceland
scp GpsMidIs-Iceland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &

c=romania
rm /tmp/$c.osm
mkfifo /tmp/$c.osm
bzcat $c.osm.bz2 > /tmp/$c.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm Romania
scp GpsMidRo-Romania-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


bzcat great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm South-Scotland
scp GpsMidScot-South-Scotland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm london
scp GpsLon-London-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  great_britain.osm.bz2 > /tmp/great_b.osm & java -mx3500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/great_b.osm NorthEngland
scp GpsNEng-NorthernEngland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & time java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm RheinMain
scp GpsMidRM-RheinMain-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & time java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm nuernberg
scp GpsMidN-Nuernberg-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Cologne
scp GpsMidKB-Koeln-Bonn-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Berlin
scp GpsMidB-Berlin-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Munich
scp GpsMun-Munich-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm hamburg
scp GpsMidHH-Hamburg-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx5500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/germany.osm Bayern
scp GpsMidBay-Bayern-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  austria.osm.bz2 > /tmp/austria.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/austria.osm Wien
scp GpsMidW-Wien-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  france.osm.bz2 > /tmp/france.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/france.osm Paris
scp GpsMidP-Paris-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  netherlands.osm.bz2 > /tmp/netherlands.osm & java -mx3600M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/netherlands.osm AmsterdamRotterdam
scp GpsMidAR-AmsterdamRotterdam-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  portugal.osm.bz2 > /tmp/portugal.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/portugal.osm Portugal
scp GpsMidPort-Portugal-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  norway.osm.bz2 > /tmp/norway.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/norway.osm SouthNorway
scp GpsMidSNor-SouthernNorway-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  czech_republic.osm.bz2 > /tmp/czech.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/czech.osm Praha
scp GpsMidPr-Praha-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  spain.osm.bz2 > /tmp/spain.osm & java -mx3500M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/spain.osm Spain
scp GpsMidSpain-Spain-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  switzerland.osm.bz2 > /tmp/switzerland.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/switzerland.osm Switzerland
scp GpsMidSwiss-Switzerland-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  belgium.osm.bz2 > /tmp/belgium.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/belgium.osm Belgium
scp GpsMidBelg-Belgium-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &
bzcat  philippines.osm.bz2 > /tmp/philippines.osm & java -mx3000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/philippines.osm Philippines
scp GpsMidPh-Philippines-0.5.09.ja* gpsmidW:/home/groups/g/gp/gpsmid/htdocs/prebuild/ &


bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx1000M -jar Osm2GpsMid-CVS.jar /tmp/germany.osm Munic-demo
scp GpsMidDemo-Munic-demo-0.5.09.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/GpsMunicDemo-munic-demo-CVS.jar &

#bzcat  germany.osm.bz2 > /tmp/germany.osm & java -mx1000M -jar Osm2GpsMid-0.4.51.jar /tmp/germany.osm Munic-demo
#scp GpsMidDemo-Munic-demo-0.4.51.jar gpsmidW:/home/groups/g/gp/gpsmid/htdocs/ &

