REM
REM The voice file f5 must be placed at %ProgramFiles(x86)%\eSpeak\espeak-data\voices\!v
set voice=f5

REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -a180 -whalb.wav -z -p79 -s180 -vde+%voice% -x -k5 "halb"
espeak -a180 -wscharf.wav -z -p79 -s200 -vde+%voice% -x -k5 "scharf"
espeak -a180 -wlinks.wav -z -p79 -s200 -vde+%voice% -x -k5 "links"
espeak -a180 -wrechts.wav -z -p79 -s180 -vde+%voice% -x -k5 "rechts"
espeak -a180 -wgeradeaus.wav -z -p79 -s200 -vde+%voice% -x -k5 "geradeaus"
espeak -a180 -win.wav -z -p79 -s190 -vde+%voice% -x -k5 "in"
espeak -a180 -wgleich.wav -z -p79 -s190 -vde+%voice% -x -k5 "gleich"
REM espeak -a180 -wweiterhin.wav -z -p79 -s200 -vde+%voice% -x -k5 "weiterhin"
espeak -a180 -w100.wav -z -p79 -s250 -vde+%voice% -x -k5 "100"
espeak -a180 -w200.wav -z -p79 -s220 -vde+%voice% -x -k5 "200"
espeak -a180 -w300.wav -z -p79 -s220 -vde+%voice% -x -k5 "300"
espeak -a180 -w400.wav -z -p79 -s180 -vde+%voice% -x -k5 "400"
espeak -a180 -w500.wav -z -p79 -s200 -vde+%voice% -x -k5 "500"
espeak -a180 -w600.wav -z -p79 -s200 -vde+%voice% -x -k5 "600"
espeak -a180 -w700.wav -z -p79 -s200 -vde+%voice% -x -k5 "700"
espeak -a180 -w800.wav -z -p79 -s200 -vde+%voice% -x -k5 "800"
espeak -a180 -wmetern.wav -z -p79 -s200 -vde+%voice% -x -k5 "Metern"
espeak -a180 -wyards.wav -z -p79 -s150 -ven+%voice% -x -k5 "Yards"
espeak -a180 -wdann.wav -z -p79 -s200 -vde+%voice% -x -k5 "dann"
espeak -a180 -wbald.wav -z -p79 -s200 -vde+%voice% -x -k5 "bald"
espeak -a180 -wnochmal.wav -z -p79 -s200 -vde+%voice% -x -k5 "nochmal"
espeak -a180 -wum.wav -z -p79 -s200 -vde+%voice% -x -k5 "um"
espeak -a180 -wauf.wav -z -p79 -s200 -vde+%voice% -x -k5 "auf"
espeak -a180 -wdie_autobahn.wav -z -p79 -s200 -vde+%voice% -x -k5 "die Autobahn"
espeak -a180 -wzu.wav -z -p79 -s200 -vde+%voice% -x -k5 "zu"
espeak -a180 -wfahren.wav -z -p79 -s200 -vde+%voice% -x -k5 "fahren"
espeak -a180 -wverlassen.wav -z -p79 -s200 -vde+%voice% -x -k5 "verlassen"
espeak -a180 -win_den_tunnel.wav -z -p79 -s180 -vde+%voice% -x -k5 "in den Tunnel!"
espeak -a180 -waus_dem_tunnel.wav -z -p79 -s190 -vde+%voice% -x -k5 "aus dem Tunnel!"
espeak -a180 -wim_kreisel.wav -z -p79 -s200 -vde+%voice% -x -k5 "im Kreisel die"
espeak -a180 -w1te.wav -z -p79 -s150 -vde+%voice% -x -k5 "erste"
espeak -a180 -w2te.wav -z -p79 -s180 -vde+%voice% -x -k5 "zweite"
espeak -a180 -w3te.wav -z -p79 -s150 -vde+%voice% -x -k5 "dritte"
espeak -a180 -w4te.wav -z -p79 -s180 -vde+%voice% -x -k5 "vierte"
espeak -a180 -w5te.wav -z -p79 -s180 -vde+%voice% -x -k5 "fuenfte"
espeak -a180 -w6te.wav -z -p79 -s180 -vde+%voice% -x -k5 "sechste"
espeak -a180 -wkreisel_abfahrt.wav -z -p79 -s200 -vde+%voice% -x -k5 "Abfahrt nehmen"
espeak -a180 -wam_ziel.wav -z -p79 -s200 -vde+%voice% -x -k5 "am Ziel angekommen"
espeak -a180 -wrichtung_pruefen.wav -z -p79 -s200 -vde+%voice% -x -k5 "Stimmt die Richtung?"
espeak -a180 -wneue_route.wav -z -p79 -s180 -vde+%voice% -x -k5 "Die Route wird neu berechnet."
espeak -a180 -wgeschwindigkeit.wav -z -p79 -s200 -vde+%voice% -x -k5 "Bitte nicht zu schnell!"
espeak -a180 -whalten_sie_sich.wav -z -p79 -s200 -vde+%voice% -x -k5 "halten Sie sich"
espeak -a180 -wfolge_strasse.wav -z -p79 -s200 -vde+%voice% -x -k5 "Dem Strassenverlauf folgen"
espeak -a180 -wueber_platz.wav -z -p79 -s150 -vde+%voice% -x -k5 "ueber den Platz"
espeak -a180 -wplatz_verlassen.wav -z -p79 -s150 -vde+%voice% -x -k5 "den Platz verlassen"
espeak -a180 -wbitte.wav -z -p79 -s180 -vde+%voice% -x -k5 "bitte"
espeak -a180 -wwenden.wav -z -p79 -s180 -vde+%voice% -x -k5 "wenden"

REM resample and increase volume of all sounds using sox from http://sox.sf.net
FOR /F "tokens=*" %%i in ('dir /b *.wav') do (
	REM fix wav file
	sox "%%i" repair.wav
	REM resample it to 8kHz with 16 bits and single channel, increase volume by 10 limited dBs
	REM sox 14.3.0 is required for --guard option
	sox --guard repair.wav -b 16 -c 1 "%%i" rate -I 8k gain -l 10
	del repair.wav
)

REM Finally convert the wav-files to AMR format
REM ShareNav's standard sound files are at 4.75kbit/s which is the lowest possible bit rate

FOR /F "tokens=*" %%i in ('dir /b *.wav') do (
	REM *** use available amr converter ***
	REM is Sony Ericsson (TM) AMR converter (can't find url anymore) there?
	if exist converter.exe (
		converter.exe wav2amr "%%i" "tempor.amr" MR475
	) else (
	REM converter from http://retrocde.sf.net is there?
		if exist code.exe (
			code "%%i" "tempor.amr" -br 4750
		)
	)

	REM only update amr file if it has changed
	fc >nul /b tempor.amr "%%~ni.amr"
	if errorlevel 1 (
		copy tempor.amr "%%~ni.amr"
	)
	del tempor.amr

	REM http://lame.sf.net
	lame %%i tempor.mp3 -b 8
	REM only update mp3 file if it has changed
	fc >nul /b tempor.mp3 "%%~ni.mp3"
	if errorlevel 1 (
		copy tempor.mp3 "%%~ni.mp3"
	)
	del tempor.mp3


	echo %%i
)

