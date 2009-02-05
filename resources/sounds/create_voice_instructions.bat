REM create wav files for all instructions using espeak from http://espeak.sf.net
espeak -whalf.wav -z -p0 -s180 -ven-r+m1 -x -k20 "Half"
espeak -whard.wav -z -p0 -s200 -ven-r+m1 -x -k20 "Hard"
espeak -wleft.wav -z -p0 -s200 -ven-r+m1 -x -k20 "Left"
espeak -wright.wav -z -p0 -s180 -ven-r+m1 -x -k20 "Right"
espeak -wstraighton.wav -z -p0 -s200 -ven-r+m1 -x -k20 "straight On"
REM espeak -wstraighton.wav -z -p0 -s200 -ven-r+m1 -x -k20 "[[streIt 0n_]]"
espeak -win.wav -z -p0 -s190 -ven-r+m0 -x -k20 "in"
espeak -wprepare.wav -z -p0 -s200 -ven-r+m1 -x -k20 "Prepare"
espeak -wcontinue.wav -z -p0 -s200 -ven-r+m0 -x -k20 "Continue"
espeak -w100.wav -z -p0 -s350 -ven-r+m1 -x -k20 "100"
espeak -w200.wav -z -p0 -s220 -ven-r+m1 -x -k20 "200"
espeak -w300.wav -z -p0 -s220 -ven-r+m1 -x -k20 "300"
espeak -w400.wav -z -p0 -s180 -ven-r+m0 -x -k20 "400"
espeak -w500.wav -z -p0 -s200 -ven-r+m1 -x -k20 "500"
espeak -w600.wav -z -p0 -s300 -ven-r+m1 -x -k20 "600"
espeak -w700.wav -z -p0 -s230 -ven-r+m1 -x -k20 "700"
espeak -w800.wav -z -p0 -s250 -ven-r+m1 -x -k20 "800"
espeak -wmeters.wav -z -p0 -s250 -ven-r+m1 -x -k20 "Meters"
espeak -wthen.wav -z -p0 -s250 -ven-r+m1 -x -k20 "then"
espeak -wsoon.wav -z -p0 -s250 -ven-r+m1 -x -k20 "soon"
espeak -wagain.wav -z -p0 -s250 -ven-r+m1 -x -k20 "again"
espeak -wto.wav -z -p0 -s250 -ven-r+m1 -x -k20 "to"
espeak -wenter_motorway.wav -z -p0 -s200 -ven-r+m1 -x -k20 "enter the motorway!"
espeak -wleave_motorway.wav -z -p0 -s200 -ven-r+m1 -x -k20 "leave the motorway!"
espeak -winto_tunnel.wav -z -p0 -s180 -ven-r+m1 -x -k20 "into the tunnel!"
espeak -wout_of_tunnel.wav -z -p0 -s150 -ven-r+m1 -x -k20 "out of the tunnel"
espeak -wrab.wav -z -p0 -s200 -ven-r+m1 -x -k20 "in the roundabout - take the"
espeak -w1st.wav -z -p0 -s150 -ven-r+m1 -x -k20 "first"
espeak -w2nd.wav -z -p0 -s180 -ven-r+m1 -x -k20 "second"
espeak -w3rd.wav -z -p0 -s150 -ven-r+m1 -x -k20 "third"
espeak -w4th.wav -z -p0 -s180 -ven-r+m1 -x -k20 "fourth"
espeak -w5th.wav -z -p0 -s180 -ven-r+m1 -x -k20 "fifth"
espeak -w6th.wav -z -p0 -s180 -ven-r+m1 -x -k20 "sixth"
espeak -wrabexit.wav -z -p0 -s200 -ven-r+m1 -x -k20 "exit"
espeak -wtarget_reached.wav -z -p0 -s150 -ven-r+m0 -x -k20 "target Reached"
espeak -wcheck_direction.wav -z -p0 -s200 -ven-r+m1 -x -k20 "check Direction!"
espeak -wroute_recalculation.wav -z -p0 -s150 -ven-r+m0 -x -k20 "Recalculating route"
espeak -wspeed_limit.wav -z -p0 -s150 -ven-r+m0 -x -k20 "Speed over limit"

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

