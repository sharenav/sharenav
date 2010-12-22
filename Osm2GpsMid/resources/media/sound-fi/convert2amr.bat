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
)