Installing ShareNav
-------------------

You need to choose the right variant of ShareNav for your phone.
If you are unsure, try <ShareNav-Generic-full-0.7-map65> or read the Wiki page
<http://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=DevicesList>  FIXME
where users have collected their experiences with different phone models.

Download the right JAR and JAD file for your variant and copy it to your
phone.
The installation process differs from phone to phone.
Usually you install by selecting the JAD file and choosing the right menu
entry from its context menu.


Map data
--------
<Osm2ShareNav-0.8.2-map72.jar> or the like is needed to create the binary map data for ShareNav.
Java 1.5 or later must be installed on your PC to run it.
The process is explained here:
<http://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Getting_started> FIXME

Alternatively, you can download pre-built map data from
<http://gpsmid.sourceforge.net/prebuild/>
The JAR files you find there contain the map data. Copy the one you want
to your phone and configure ShareNav to read from this file in Setup -> Map
source. (For some devices, especially from Nokia, you MUST rename 
the .jar file to .zip to make it visible and selectable.)

J2MEPolish modification for Android
-----------------------------------

To be able to draw areas faster on Android, ShareNav needs J2MEPolish
later than 2.3 or daily not older than 2012-08-22.
