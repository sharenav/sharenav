REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -a200 -whalf.wav -z -p80 -s180 -ven+f1 -x -k5 "half"
espeak -a200 -whard.wav -z -p80 -s200 -ven+f1 -x -k5 "Hard"
espeak -a200 -wleft.wav -z -p70 -s200 -ven+f1 -x -k5 "left"
espeak -a200 -wright.wav -z -p70 -s180 -ven+f1 -x -k5 "right"
espeak -a200 -wstraighton.wav -z -p80 -s200 -ven+f1 -x -k5 "Straight on"
REM espeak -a200 -wstraighton.wav -z -p80 -s200 -ven+f1 -x -k5 "[[streIt 0n_]]"
espeak -a200 -win.wav -z -p70 -s190 -ven+f1 -x -k5 "in"
espeak -a200 -wprepare.wav -z -p70 -s200 -ven+f1 -x -k5 "Prepare"
espeak -a200 -wcontinue.wav -z -p80 -s200 -ven+f1 -x -k5 "Continue"
espeak -a200 -w100.wav -z -p80 -s350 -ven+f1 -x -k5 "100"
espeak -a200 -w200.wav -z -p80 -s220 -ven+f1 -x -k5 "200"
espeak -a200 -w300.wav -z -p80 -s220 -ven+f1 -x -k5 "300"
espeak -a200 -w400.wav -z -p80 -s180 -ven+f1 -x -k5 "400"
espeak -a200 -w500.wav -z -p80 -s200 -ven+f1 -x -k5 "500"
espeak -a200 -w600.wav -z -p80 -s300 -ven+f1 -x -k5 "600"
espeak -a200 -w700.wav -z -p80 -s230 -ven+f1 -x -k5 "700"
espeak -a200 -w800.wav -z -p80 -s250 -ven+f1 -x -k5 "800"
espeak -a200 -wmeters.wav -z -p70 -s250 -ven+f1 -x -k5 "Meters"
espeak -a200 -wthen.wav -z -p70 -s250 -ven+f1 -x -k5 "then"
espeak -a200 -wsoon.wav -z -p70 -s250 -ven+f1 -x -k5 "soon"
espeak -a200 -wagain.wav -z -p80 -s250 -ven+f1 -x -k5 "again"
espeak -a200 -wto.wav -z -p70 -s250 -ven+f1 -x -k5 "to"
espeak -a200 -wenter_motorway.wav -z -p80 -s200 -ven+f1 -x -k5 "enter the motorway!"
espeak -a200 -wleave_motorway.wav -z -p80 -s200 -ven+f1 -x -k5 "leave the motorway!"
espeak -a200 -winto_tunnel.wav -z -p80 -s180 -ven+f1 -x -k5 "into the tunnel!"
espeak -a200 -wout_of_tunnel.wav -z -p80 -s190 -ven+f1 -x -k5 "Out of the tunnel"
espeak -a200 -wrab.wav -z -p80 -s200 -ven+f1 -x -k5 "in the roundabout - take the"
espeak -a200 -w1st.wav -z -p80 -s150 -ven+f1 -x -k5 "first"
espeak -a200 -w2nd.wav -z -p80 -s180 -ven+f1 -x -k5 "second"
espeak -a200 -w3rd.wav -z -p80 -s150 -ven+f1 -x -k5 "third"
espeak -a200 -w4th.wav -z -p80 -s180 -ven+f1 -x -k5 "fourth"
espeak -a200 -w5th.wav -z -p80 -s180 -ven+f1 -x -k5 "fifth"
espeak -a200 -w6th.wav -z -p80 -s180 -ven+f1 -x -k5 "sixth"
espeak -a200 -wrabexit.wav -z -p70 -s250 -ven+f1 -x -k5 "exit"
espeak -a200 -wtarget_reached.wav -z -p70 -s180 -ven+f1 -x -k5 "target Reached"
espeak -a200 -wcheck_direction.wav -z -p70 -s200 -ven+f1 -x -k5 "check Direction!"
espeak -a200 -wroute_recalculation.wav -z -p70 -s180 -ven+f1 -x -k5 "Recalculating route"
espeak -a200 -wspeed_limit.wav -z -p80 -s150 -ven+f1 -x -k5 "Speed over limit"
espeak -a200 -wbear.wav -z -p80 -s150 -ven+f1 -x -k5 "bear"
espeak -a200 -wfollow_street.wav -z -p80 -s150 -ven+f1 -x -k5 "follow the street until further notice"

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
	echo %%i
)

