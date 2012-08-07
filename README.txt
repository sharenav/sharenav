Installing GpsMid
-----------------
You need to choose the right variant of GpsMid for your phone.
If you are unsure, try <GpsMid-Generic-full-0.7-map65> or read the Wiki page
<http://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=DevicesList> 
where users have collected their experiences with different phone models.

Download the right JAR and JAD file for your variant and copy it to your
phone.
The installation process differs from phone to phone.
Usually you install by selecting the JAD file and choosing the right menu
entry from its context menu.


Map data
--------
<Osm2GpsMid-0.7-map65.jar> is needed to create the binary map data for GpsMid.
Java 1.5 or later must be installed on your PC to run it.
The process is explained here:
<http://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Getting_started>

Alternatively, you can download pre-built map data from
<http://gpsmid.sourceforge.net/prebuild/>
The JAR files you find there contain the map data. Copy the one you want
to your phone and configure GpsMid to read from this file in Setup -> Map
source. (For some devices, especially from Nokia, you MUST rename 
the .jar file to .zip to make it visible and selectable.)

J2MEPolish modification for Android
-----------------------------------

To be able to draw areas faster on Android, a modification can be made
to J2MEPolish 2.3.  The modification is made to a part of J2MEPolish
which is source code to be compiled in the J2MEPolish build process
for Android, so a recompilation of J2MEPolish is not required for
applying the modification.

The file to modify is the Graphics.java file inside the jar
lib/enough-j2mepolish-build.jar in the J2MEPolish distribution.
For example, if J2MEPolish is installed under /usr/local, the
absolute path to the modified jar is:

/usr/local/J2ME-Polish-2.3/lib/enough-j2mepolish-build.jar

Inside the jar, the path to the modified file is:

src/de/enough/polish/android/lcdui/Graphics.java

The modification (can be applied e.g. with the "patch" command, or
manually; change consists of adding two methods to expose the Android
canvas & paint objects to GpsMid):

diff -u -r distrib-src/de/enough/polish/android/lcdui/Graphics.java src/de/enough/polish/android/lcdui/Graphics.java
--- distrib-src/de/enough/polish/android/lcdui/Graphics.java	2012-01-31 21:47:54.000000000 +0200
+++ src/de/enough/polish/android/lcdui/Graphics.java	2012-08-07 12:05:43.130756995 +0300
@@ -1595,6 +1595,13 @@
 		this.paint.setStyle(Style.STROKE);
 	}
 
+	public Canvas getCanvas() {
+		return this.canvas;
+	}
+	public Paint getPaint() {
+		return this.paint;
+	}
+
 	/**
 	 * Renders a series of device-independent RGB+transparency values in a
 	 * specified region.  The values are stored in

Using the J2MEPolish modification
---------------------------------

Using Osm2GpsMid
================

Osm2GpsMid contains .apks for both varieties - the plain ones are without area
outlines, those with "-outlines" suffix are build with the outline feature active.

Building from source
====================

By default, if you build GpsMid from source, it doesn't require or
use the J2MEPolish modification.

To use the area outline code, modify J2MEPolish, then modify build.xml
to include the "-outlines" targets.  This happens by moving the
"-outlines" -containing lines above the "<!--" mark, in other word by
uncommenting these lines.
