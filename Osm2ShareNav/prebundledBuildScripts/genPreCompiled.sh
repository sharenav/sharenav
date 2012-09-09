#!/bin/bash

ulimit -Sv 14000000
renice 15 $$

function genPBF {
    # $1 = name of PBF file w/o ext, $2 = name of bundle file w/o ext, $3 = Midlet name suffix
    echo "========================" >> build.log
    echo -e "Processing $2, midlet name ShareNav$3-0.7.7-map69\n\n" >> build.log
    echo "Processing $2, midlet name ShareNav$3-0.7.7-map69"
    java -mx8000M -XX:NewRatio=32 -jar Osm2ShareNav-CVS.jar --cellID=cells.txt.gz /home/apmon/planet_dumps/$1.osm.pbf $2 >> build.log 2>&1
    if [ -n "$PASS" ]; then
	java -jar JadTool.jar -addjarsig -keypass $PASS -alias fossgis-key -keystore java-keystore.jks -inputjad ShareNav$3-0.7.7-map69.jad -outputjad ShareNav$3-0.7.7-map69.jad -jarfile ShareNav$3-0.7.7-map69.jar
	java -jar JadTool.jar -addcert -alias fossgis-key -keystore java-keystore.jks -inputjad ShareNav$3-0.7.7-map69.jad -outputjad ShareNav$3-0.7.7-map69.jad
    fi
}

function genBZ2 {
    # $1 = name of BZ2 file w/o ext, $2 = name of bundle file w/o ext, $3 = Midlet name suffix
    echo "========================" >> build.log
    echo -e "Processing $2, midlet name ShareNav$3-0.7.7-map69\n\n" >> build.log
    echo "Processing $2, midlet name ShareNav$3-0.7.7-map69"
    c=$1
    rm /tmp/$c.osm
    mkfifo /tmp/$c.osm
    bzcat /home/apmon/planet_dumps/$c.osm.bz2 > /tmp/$c.osm & java -mx5000M -jar Osm2ShareNav-CVS.jar --cellID=cells.txt.gz /tmp/$c.osm $2 >> build.log 2>&1
    if [ -n "$PASS" ]; then
	java -jar JadTool.jar -addjarsig -keypass $PASS -alias fossgis-key -keystore java-keystore.jks -inputjad ShareNav$3-0.7.7-map69.jad -outputjad ShareNav$3-0.7.7-map69.jad -jarfile ShareNav$3-0.7.7-map69.jar
	java -jar JadTool.jar -addcert -alias fossgis-key -keystore java-keystore.jks -inputjad ShareNav$3-0.7.7-map69.jad -outputjad ShareNav$3-0.7.7-map69.jad
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


cd /home/apmon/Osm2ShareNav

rm build.log

# Africa
genPBF canary Canary Canary
genPBF congo_democratic_republic Congo Cd
genBZ2 egypt Egypt Eg
genPBF ethiopia Ethiopia Et
genBZ2 kenya Kenya Ke
genPBF libya Libya Ly
genPBF morocco Morocco Ma
genBZ2 namibia Namibia Na
genPBF nigeria Nigeria Ne
genBZ2 seychelles Seychelles Sc
genPBF south_africa_and_lesotho South_Africa Za
genBZ2 tunisia Tunesia Tn
genBZ2 zimbabwe Zimbabwe Zw

# Americas
genPBF argentina Argentina Ar
genPBF brazil Brazil Br
genPBF british-columbia BritishColumbia BC
genPBF california BayArea BayArea
genPBF california LosAngeles LA
genPBF colombia Colombia Co
genPBF colorado Boulder Boulder
genPBF colorado Colorado Col
genPBF colorado Denver Den
genPBF haiti-and-domrep Haiti Ht
genPBF louisiana NewOrleans NO
genBZ2 jamaica Jamaica Jm
genPBF mexico Mexico Mx
genPBF new-york NYC NYC
genPBF ontario Ontario ON
genBZ2 puerto_rico PuertoRico Pr
genBZ2 trinidad_and_tobago Trinidad Tt
genBZ2 venezuela Venezuela Ve

# Asia
genBZ2 afghanistan Afghanistan Af
genBZ2 bahrain Bahrain Bh
genBZ2 bangladesh Bangladesh Bd
genPBF china China Cn
genBZ2 cyprus Cyprus Cy
genPBF india India In
genPBF india SouthIndia SIn
genPBF indonesia Indonesia Id
genPBF iran Iran Ir
genPBF iraq Iraq Iq
genPBF israel Israel Il
# Runs out of memory: genPBF japan Japan Jp
genBZ2 jordan Jordan Jo
genPBF kazakhstan Kazakhstan Kz
genBZ2 kuwait Kuwait Kw
genBZ2 lebanon Lebanon Lb
genPBF malaysia_singapore_brunei MalaysiaSingaporeBrunei MySgBn
genBZ2 myanmar Myanmar Mm
genPBF pakistan Pakistan Pk
genPBF philippines Philippines Ph
genBZ2 saudi_arabia Saudi Sa
genBZ2 south_korea South_Korea Kr
genBZ2 sri_lanka Sri_Lanka Lk
genBZ2 syria Syria Sy
genBZ2 thailand Thailand Th 
genPBF turkey Turkey Tr
genBZ2 united_arab_emirates UAE UAE

# Australia
genPBF australia-oceania Australia Au
genPBF australia-oceania NewZealand Nz
genPBF australia-oceania CentralAustralia CA
genPBF australia-oceania WesternAustralia WA
genPBF australia-oceania Queensland Qu
genPBF australia-oceania Victoria Vic
genPBF australia-oceania NSW NSW

# Europe
genPBF albania Albania Al
genPBF armenia Armenia Am
genPBF austria Austria At
genPBF austria Wien W
genPBF belarus Belarus By
genPBF belgium Belgium Be
genPBF bosnia-herzegovina Bosnia-Herzegovina Ba
genPBF bulgaria Bulgaria Bg
genPBF croatia Croatia Hr
genPBF czech_republic Czech Cz
genPBF czech_republic Praha Praha
genPBF denmark Denmark Dk
genPBF estonia Estonia Ee
genPBF finland Finland Fi
genPBF finland Helsinki Hel
genPBF georgia Georgia Ge
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
genPBF great_britain South-Scotland Scot
genPBF great_britain SouthWestEngland SWE
genPBF great_britain london Lon
genPBF great_britain NorthEngland NEng
genPBF greece Greece Gr
genPBF hungary Hungary Hu
genPBF iceland Iceland Is
genPBF ile-de-france Paris P
genPBF ireland Ireland Ie
genPBF italy Rome Rome
genPBF italy Milano Milano
genPBF italy Torino Torino
genPBF italy SouthItaly SIt
genPBF italy NorthItaly NIt
genPBF italy CentralItaly CIt
genPBF kosovo Kosovo Kos
genPBF latvia Latvia Lv
genPBF lithuania Lithuania Lt
genPBF luxembourg Luxembourg Lu
genPBF macedonia Macedonia Mk
genPBF malta Malta Mt
genPBF moldova Moldova Md
genPBF netherlands Netherlands Nl
genPBF netherlands Amsterdam Amst
genPBF netherlands AmsterdamRotterdam AmstRott
genPBF norway SouthNorway SNor
genPBF poland Poland Pl
genPBF poland Warsaw Warsaw
genPBF poland Krakow Krakow
genPBF poland Lodz Lodz
genPBF portugal Portugal Pt
genPBF romania Romania Ro
genPBF russia-european-part moscow Mos
genPBF russia-european-part StPetersburg StPeter
genPBF russia-european-part Russia_european Rue
genPBF slovakia Slovakia Sk
genPBF slovenia Slovenia Si
genPBF spain Spain Es
genPBF spain Madrid Mad
genPBF sweden Sweden Se
genPBF switzerland Switzerland Ch
genPBF ukraine Ukraine Ua

rm /home/apmon/public_html/ShareNav/ShareNav*.ja*

cp -p *.ja* /home/apmon/public_html/ShareNav/
