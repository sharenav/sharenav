REM
REM The voice file f5 must be placed at %ProgramFiles(x86)%\eSpeak\espeak-data\voices\!v
set voice=f5

REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -a180 -whalf.wav -z -p79 -s180 -ven+%voice% -x -k5 "half"
espeak -a180 -whard.wav -z -p79 -s200 -ven+%voice% -x -k5 "hard"
espeak -a180 -wleft.wav -z -p79 -s200 -ven+%voice% -x -k5 "left"
espeak -a180 -wright.wav -z -p79 -s180 -ven+%voice% -x -k5 "right"
espeak -a180 -wstraighton.wav -z -p79 -s200 -ven+%voice% -x -k5 "straight on"
REM espeak -a180 -wstraighton.wav -z -p79 -s200 -ven+%voice% -x -k5 "[[streIt 0n_]]"
espeak -a180 -win.wav -z -p79 -s190 -ven+%voice% -x -k5 "in"
espeak -a180 -wprepare.wav -z -p79 -s190 -ven+%voice% -x -k5 "prepare"
REM espeak -a180 -wcontinue.wav -z -p79 -s200 -ven+%voice% -x -k5 "Continue"
espeak -a180 -w100.wav -z -p79 -s250 -ven+%voice% -x -k5 "100"
espeak -a180 -w200.wav -z -p79 -s220 -ven+%voice% -x -k5 "200"
espeak -a180 -w300.wav -z -p79 -s220 -ven+%voice% -x -k5 "300"
espeak -a180 -w400.wav -z -p79 -s180 -ven+%voice% -x -k5 "400"
espeak -a180 -w500.wav -z -p79 -s200 -ven+%voice% -x -k5 "500"
espeak -a180 -w600.wav -z -p79 -s200 -ven+%voice% -x -k5 "600"
espeak -a180 -w700.wav -z -p79 -s200 -ven+%voice% -x -k5 "700"
espeak -a180 -w800.wav -z -p79 -s200 -ven+%voice% -x -k5 "800"
espeak -a180 -wmeters.wav -z -p79 -s200 -ven+%voice% -x -k5 "meters"
espeak -a180 -wyards.wav -z -p79 -s150 -ven+%voice% -x -k5 "yards"
espeak -a180 -wthen.wav -z -p79 -s200 -ven+%voice% -x -k5 "then"
espeak -a180 -wsoon.wav -z -p79 -s200 -ven+%voice% -x -k5 "soon"
espeak -a180 -wagain.wav -z -p79 -s220 -ven+%voice% -x -k5 "again"
espeak -a180 -wto.wav -z -p79 -s200 -ven+%voice% -x -k5 "to"
espeak -a180 -wenter_motorway.wav -z -p79 -s200 -ven+%voice% -x -k5 "enter the motorway!"
espeak -a180 -wleave_motorway.wav -z -p79 -s200 -ven+%voice% -x -k5 "leave the motorway!"
espeak -a180 -winto_tunnel.wav -z -p79 -s180 -ven+%voice% -x -k5 "into the tunnel!"
espeak -a180 -wout_of_tunnel.wav -z -p79 -s190 -ven+%voice% -x -k5 "out of the tunnel"
espeak -a180 -wrab.wav -z -p79 -s200 -ven+%voice% -x -k5 "in the roundabout - take the"
espeak -a180 -w1st.wav -z -p79 -s150 -ven+%voice% -x -k5 "first"
espeak -a180 -w2nd.wav -z -p79 -s180 -ven+%voice% -x -k5 "second"
espeak -a180 -w3rd.wav -z -p79 -s150 -ven+%voice% -x -k5 "third"
espeak -a180 -w4th.wav -z -p79 -s180 -ven+%voice% -x -k5 "fourth"
espeak -a180 -w5th.wav -z -p79 -s180 -ven+%voice% -x -k5 "fifth"
espeak -a180 -w6th.wav -z -p79 -s180 -ven+%voice% -x -k5 "sixth"
espeak -a180 -wrabexit.wav -z -p79 -s250 -ven+%voice% -x -k5 "exit"
espeak -a180 -wdest_reached.wav -z -p79 -s150 -ven+%voice% -x -k5 "destination reached"
espeak -a180 -wcheck_direction.wav -z -p79 -s200 -ven+%voice% -x -k5 "check direction!"
espeak -a180 -wroute_recalculation.wav -z -p79 -s180 -ven+%voice% -x -k5 "recalculating route"
espeak -a180 -wspeed_limit.wav -z -p79 -s150 -ven+%voice% -x -k5 "Speed over limit"
espeak -a180 -wbear.wav -z -p79 -s150 -ven+%voice% -x -k5 "bear"
espeak -a180 -wfollow_street.wav -z -p79 -s150 -ven+%voice% -x -k5 "follow the street until further notice"
espeak -a180 -warea_cross.wav -z -p79 -s150 -ven+%voice% -x -k5 "cross the area"
espeak -a180 -warea_crossed.wav -z -p79 -s150 -ven+%voice% -x -k5 "leave the area"
espeak -a180 -wuturn.wav -z -p79 -s100 -ven+%voice% -x -k5 "u-turn"

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

