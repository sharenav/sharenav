#!/bin/bash

ulimit -Sv 14000000
renice 15 $$

function genPBF {
    echo "========================" >> build.log
    echo -e "Processing $2, midlet name GpsMid$3-0.7.7-map69\n\n" >> build.log
    echo "Processing $2, midlet name GpsMid$3-0.7.7-map69"
    java -mx8000M -XX:NewRatio=32 -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/$1.osm.pbf $2 >> build.log 2>&1
    if [ -n "$PASS" ]; then
	java -jar JadTool.jar -addjarsig -keypass $PASS -alias fossgis-key -keystore java-keystore.jks -inputjad GpsMid$3-0.7.7-map69.jad -outputjad GpsMid$3-0.7.7-map69.jad -jarfile GpsMid$3-0.7.7-map69.jar
	java -jar JadTool.jar -addcert -alias fossgis-key -keystore java-keystore.jks -inputjad GpsMid$3-0.7.7-map69.jad -outputjad GpsMid$3-0.7.7-map69.jad
    fi
}

function genBZ2 {
    echo "========================" >> build.log
    echo -e "Processing $2, midlet name GpsMid$3-0.7.7-map69\n\n" >> build.log
    echo "Processing $2, midlet name GpsMid$3-0.7.7-map69"
    c=$1
    rm /tmp/$c.osm
    mkfifo /tmp/$c.osm
    bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2GpsMid-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm $2 >> build.log 2>&1
    if [ -n "$PASS" ]; then
	java -jar JadTool.jar -addjarsig -keypass $PASS -alias fossgis-key -keystore java-keystore.jks -inputjad GpsMid$3-0.7.7-map69.jad -outputjad GpsMid$3-0.7.7-map69.jad -jarfile GpsMid$3-0.7.7-map69.jar
	java -jar JadTool.jar -addcert -alias fossgis-key -keystore java-keystore.jks -inputjad GpsMid$3-0.7.7-map69.jad -outputjad GpsMid$3-0.7.7-map69.jad
    fi
    rm /tmp/$c.osm
}


skip=0;
idx=1;
idx=$[$idx+1];
skip=$[$skip+1];

echo $idx
echo $skip

echo "Please enter the password for the signing certificate"
read -s PASS


cd /home/apmon/Osm2GpsMid

rm build.log

genPBF malta Malta Mt
genBZ2 trinidad Trinidad Tt
genBZ2 uae UAE UAE
genBZ2 afghanistan Afghanistan Af
genBZ2 jamaica Jamaica Jm
genBZ2 bangladesh Bangladesh Bd
genBZ2 mexico Mexico Mx
genBZ2 venezuela Venezuela Ve
genBZ2 lebanon Lebanon Lb
genBZ2 singapore Singapore Sg
genBZ2 jordan Jordan Jo
genBZ2 syria Syria Sy
genBZ2 bahrain Bahrain Bh
genBZ2 brunei Brunai Bn
genPBF colombia Colombia Co
genPBF argentina Argentina Ar
genPBF iraq Iraq Iq
genPBF kazakhstan Kazakhstan Kz
genPBF canary Canary Canary
genPBF czech_republic Czech Cz
genPBF czech_republic Praha
genPBF spain Spain Es
genPBF spain Madrid Mad
genPBF haiti Haiti Ht
genPBF indonesia Indonesia Id
genPBF australia-oceania Australia Au
genPBF australia-oceania NewZealand Nz
genPBF australia-oceania CentralAustralia CA
genPBF australia-oceania WesternAustralia WA
genPBF australia-oceania Queensland Qu
genPBF australia-oceania Victoria Vic
genPBF australia-oceania NSW NSW
genBZ2 sri_lanka Sri_Lanka Lk
genPBF louisiana NewOrleans NO
genPBF colorado Colorado Col
genPBF colorado Denver Den
genPBF colorado Boulder Boulder
genPBF california BayArea BayArea
genPBF california LosAngeles LA
genPBF new_york NYC NYC
genBZ2 puerto_rico PuertoRico Pr
genBZ2 britishcolumbia BritishColumbia BC
genPBF Ontario Ontario ON
genPBF poland Poland Pl
genPBF poland Warsaw Warsaw
genPBF poland Krakow Krakow
genPBF poland Lodz Lodz
genPBF sweden Sweden Se
genPBF belarus Belarus By
genBZ2 albania Albania Al
genBZ2 croatia Croatia Hr
genBZ2 bosnia Bosnia-Herzegovina Ba
genBZ2 bulgaria Bulgaria Bg
genPBF estonia Estonia Ee
genBZ2 kosovo Kosovo Kos
genBZ2 lithuania Lithuania Lt
genBZ2 latvia Latvia Lv
genBZ2 luxembourg Luxembourg Lu
genBZ2 macedonia Macedonia Mk
genBZ2 moldova Moldova Md
genPBF slovakia Slovakia Sk
genBZ2 slovenia Slovenia Si
genPBF finland Finland Fi
genPBF finland Helsinki Hel
genPBF libya Libya Ly
genPBF netherlands Netherlands Nl
genPBF netherlands Amsterdam Amst
genPBF netherlands AmsterdamRotterdam AR
genPBF italy Rome Rome
genPBF italy Milano Milano
genPBF italy Torino Torino
genPBF italy SouthItaly SIt
genPBF italy NorthItaly NIt
genPBF italy CentralItaly CIt
genBZ2 congo Congo Cg
genBZ2 kenya Kenya Ke
genBZ2 seychelles Seychelles Sc
genBZ2 namibia Namibia Na
genBZ2 tunesia Tunesia Tn
genPBF brazil Brazil Br
genPBF china China Cn
genBZ2 malaysia Malaysia My
genBZ2 thailand Thailand Th 
genBZ2 saudi_arabia Saudi Sa
genBZ2 kuwait Kuwait Kw
genPBF south_africa South_Africa Za
genPBF russia_europe moscow Mos
genPBF russia_europe StPetersburg StPeter
genPBF russia_europe Russia_european Rue
genPBF denmark Denmark Dk
genBZ2 zimbabwe Zimbabwe Zw
genBZ2 south_korea South_Korea Kr
genPBF pakistan Pakistan Pk
genPBF india India In
genPBF india SouthIndia SIn
genPBF israel Israel Il
genBZ2 iran Iran Ir
genPBF ukraine Ukraine Ua
genPBF turkey Turkey Tr
genPBF ireland Ireland Ie
genPBF hungary Hungary Hu
genBZ2 cyprus Cyprus Cy
genPBF greece Greece Gr
genBZ2 iceland Iceland Is
genPBF romania Romania Ro
genPBF great_britain South-Scotland Scot
genPBF great_britain SouthWestEngland SWE
genPBF great_britain london Lon
genPBF great_britain NorthEngland NEng
genPBF germany Stuttgart St
genPBF germany Schleswig SW
genPBF germany RheinMain RM
genPBF germany nuernberg N
genPBF germany Cologne KB
genPBF germany Berlin B
genPBF germany Munich Mun
genPBF germany hamburg HH
genPBF germany Bayern Bay
genPBF germany Karlsruhe Ka
genPBF germany Leipzig Leip
genPBF germany RuhrGebiet Ruhr
genPBF austria Wien W
genPBF austria Austria At
genPBF france Paris P
genPBF portugal Portugal Pt
genPBF norway SouthNorway SNor
genPBF switzerland Switzerland Ch
genPBF belgium Belgium Be
genPBF philippines Philippines Ph
genPBF georgia Georgia Ge
genPBF armenia Armenia Am
genBZ2 myanmar Myanmar Mm

rm /home/apmon/public_html/GpsMid/GpsMid*.ja*

cp -p *.ja* /home/apmon/public_html/GpsMid/

