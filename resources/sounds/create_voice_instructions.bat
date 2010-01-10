REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -a120 -whalf.wav -z -p79 -s180 -ven+f2 -x -k5 "half"
espeak -a120 -whard.wav -z -p79 -s200 -ven+f2 -x -k5 "hard"
espeak -a120 -wleft.wav -z -p79 -s200 -ven+f2 -x -k5 "left"
espeak -a120 -wright.wav -z -p79 -s180 -ven+f2 -x -k5 "right"
espeak -a120 -wstraighton.wav -z -p79 -s200 -ven+f2 -x -k5 "straight on"
REM espeak -a120 -wstraighton.wav -z -p79 -s200 -ven+f2 -x -k5 "[[streIt 0n_]]"
espeak -a120 -win.wav -z -p79 -s190 -ven+f2 -x -k5 "in"
espeak -a120 -wprepare.wav -z -p79 -s190 -ven+f2 -x -k5 "prepare"
REM espeak -a120 -wcontinue.wav -z -p79 -s200 -ven+f2 -x -k5 "Continue"
espeak -a120 -w100.wav -z -p79 -s250 -ven+f2 -x -k5 "100"
espeak -a120 -w200.wav -z -p79 -s220 -ven+f2 -x -k5 "200"
espeak -a120 -w300.wav -z -p79 -s220 -ven+f2 -x -k5 "300"
espeak -a120 -w400.wav -z -p79 -s180 -ven+f2 -x -k5 "400"
espeak -a120 -w500.wav -z -p79 -s200 -ven+f2 -x -k5 "500"
espeak -a120 -w600.wav -z -p79 -s200 -ven+f2 -x -k5 "600"
espeak -a120 -w700.wav -z -p79 -s200 -ven+f2 -x -k5 "700"
espeak -a120 -w800.wav -z -p79 -s200 -ven+f2 -x -k5 "800"
espeak -a120 -wmeters.wav -z -p79 -s200 -ven+f2 -x -k5 "meters"
espeak -a120 -wthen.wav -z -p79 -s200 -ven+f2 -x -k5 "then"
espeak -a120 -wsoon.wav -z -p79 -s200 -ven+f2 -x -k5 "soon"
espeak -a120 -wagain.wav -z -p79 -s200 -ven+f2 -x -k5 "again"
espeak -a120 -wto.wav -z -p79 -s200 -ven+f2 -x -k5 "to"
espeak -a120 -wenter_motorway.wav -z -p79 -s200 -ven+f2 -x -k5 "enter the motorway!"
espeak -a120 -wleave_motorway.wav -z -p79 -s200 -ven+f2 -x -k5 "leave the motorway!"
espeak -a120 -winto_tunnel.wav -z -p79 -s180 -ven+f2 -x -k5 "into the tunnel!"
espeak -a120 -wout_of_tunnel.wav -z -p79 -s190 -ven+f2 -x -k5 "out of the tunnel"
espeak -a120 -wrab.wav -z -p79 -s200 -ven+f2 -x -k5 "in the roundabout - take the"
espeak -a120 -w1st.wav -z -p79 -s150 -ven+f2 -x -k5 "first"
espeak -a120 -w2nd.wav -z -p79 -s180 -ven+f2 -x -k5 "second"
espeak -a120 -w3rd.wav -z -p79 -s150 -ven+f2 -x -k5 "third"
espeak -a120 -w4th.wav -z -p79 -s180 -ven+f2 -x -k5 "fourth"
espeak -a120 -w5th.wav -z -p79 -s180 -ven+f2 -x -k5 "fifth"
espeak -a120 -w6th.wav -z -p79 -s180 -ven+f2 -x -k5 "sixth"
espeak -a120 -wrabexit.wav -z -p79 -s250 -ven+f2 -x -k5 "exit"
espeak -a120 -wdest_reached.wav -z -p79 -s150 -ven+f2 -x -k5 "destination reached"
espeak -a120 -wcheck_direction.wav -z -p79 -s200 -ven+f2 -x -k5 "check direction!"
espeak -a120 -wroute_recalculation.wav -z -p79 -s180 -ven+f2 -x -k5 "recalculating route"
espeak -a120 -wspeed_limit.wav -z -p79 -s150 -ven+f2 -x -k5 "speed over limit"
espeak -a120 -wbear.wav -z -p79 -s150 -ven+f2 -x -k5 "bear"
espeak -a120 -wfollow_street.wav -z -p79 -s150 -ven+f2 -x -k5 "follow the street until further notice"
espeak -a120 -warea_cross.wav -z -p79 -s150 -ven+f2 -x -k5 "cross the area"
espeak -a120 -warea_crossed.wav -z -p79 -s150 -ven+f2 -x -k5 "leave the area"
espeak -a120 -wuturn.wav -z -p79 -s100 -ven+f2 -x -k5 "u-turn"

REM resample and increase volume of all sounds using sox from http://sox.sf.net
FOR /F "tokens=*" %%i in ('dir /b *.wav') do (
	REM fix wav file
	sox "%%i" repair.wav
	REM resample it to 8kHz with 16 bits and single channel, increase volume by 50 %
        sox repair.wav -b 16 -c 1 -r 8k "%%i" vol 1.50
	del repair.wav
)

REM Finally convert the wav-files to AMR format
REM GpsMid's standard sound files are at 4.75kbit/s which is the lowest possible bit rate

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

