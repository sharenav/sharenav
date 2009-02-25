REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -whalf.wav -z -p80 -s180 -ven+f1 -x -k5 "half"
espeak -whard.wav -z -p80 -s200 -ven+f1 -x -k5 "Hard"
espeak -wleft.wav -z -p70 -s200 -ven+f1 -x -k5 "left"
espeak -wright.wav -z -p70 -s180 -ven+f1 -x -k5 "right"
espeak -wstraighton.wav -z -p80 -s200 -ven+f1 -x -k5 "Straight on"
REM espeak -wstraighton.wav -z -p80 -s200 -ven+f1 -x -k5 "[[streIt 0n_]]"
espeak -win.wav -z -p70 -s190 -ven+f1 -x -k5 "in"
espeak -wprepare.wav -z -p70 -s200 -ven+f1 -x -k5 "Prepare"
espeak -wcontinue.wav -z -p80 -s200 -ven+f1 -x -k5 "Continue"
espeak -w100.wav -z -p80 -s350 -ven+f1 -x -k5 "100"
espeak -w200.wav -z -p80 -s220 -ven+f1 -x -k5 "200"
espeak -w300.wav -z -p80 -s220 -ven+f1 -x -k5 "300"
espeak -w400.wav -z -p80 -s180 -ven+f1 -x -k5 "400"
espeak -w500.wav -z -p80 -s200 -ven+f1 -x -k5 "500"
espeak -w600.wav -z -p80 -s300 -ven+f1 -x -k5 "600"
espeak -w700.wav -z -p80 -s230 -ven+f1 -x -k5 "700"
espeak -w800.wav -z -p80 -s250 -ven+f1 -x -k5 "800"
espeak -wmeters.wav -z -p70 -s250 -ven+f1 -x -k5 "Meters"
espeak -wthen.wav -z -p70 -s250 -ven+f1 -x -k5 "then"
espeak -wsoon.wav -z -p70 -s250 -ven+f1 -x -k5 "soon"
espeak -wagain.wav -z -p80 -s250 -ven+f1 -x -k5 "again"
espeak -wto.wav -z -p70 -s250 -ven+f1 -x -k5 "to"
espeak -wenter_motorway.wav -z -p80 -s200 -ven+f1 -x -k5 "enter the motorway!"
espeak -wleave_motorway.wav -z -p80 -s200 -ven+f1 -x -k5 "leave the motorway!"
espeak -winto_tunnel.wav -z -p80 -s180 -ven+f1 -x -k5 "into the tunnel!"
espeak -wout_of_tunnel.wav -z -p80 -s190 -ven+f1 -x -k5 "Out of the tunnel"
espeak -wrab.wav -z -p80 -s200 -ven+f1 -x -k5 "in the roundabout - take the"
espeak -w1st.wav -z -p80 -s150 -ven+f1 -x -k5 "first"
espeak -w2nd.wav -z -p80 -s180 -ven+f1 -x -k5 "second"
espeak -w3rd.wav -z -p80 -s150 -ven+f1 -x -k5 "third"
espeak -w4th.wav -z -p80 -s180 -ven+f1 -x -k5 "fourth"
espeak -w5th.wav -z -p80 -s180 -ven+f1 -x -k5 "fifth"
espeak -w6th.wav -z -p80 -s180 -ven+f1 -x -k5 "sixth"
espeak -wrabexit.wav -z -p70 -s250 -ven+f1 -x -k5 "exit"
espeak -wtarget_reached.wav -z -p70 -s180 -ven+f1 -x -k5 "target Reached"
espeak -wcheck_direction.wav -z -p70 -s200 -ven+f1 -x -k5 "check Direction!"
espeak -wroute_recalculation.wav -z -p70 -s180 -ven+f1 -x -k5 "Recalculating route"
espeak -wspeed_limit.wav -z -p80 -s150 -ven+f1 -x -k5 "Speed over limit"
espeak -wbear.wav -z -p80 -s150 -ven+f1 -x -k5 "bear"

REM resample and increase volume of all sounds using sox from http://sox.sf.net
FOR /F "tokens=*" %%i in ('dir /b *.wav') do (
	REM fix wav file
	sox "%%i" repair.wav
	REM increase volume of wav file by 30 dB limited to 0.05
	REM and resample it to 8kHz with 16 bits and single channel
        sox repair.wav -b 16 -c 1 -r 8k "%%i" vol 30dB 0.05
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

