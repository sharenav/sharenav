@if not exist "%ProgramFiles%\inkscape\inkscape.exe" (
	@if not exist "%ProgramFiles(x86)%\inkscape\inkscape.exe" (		
		@ echo Inkscape from http://www.inkscape.org/download/ is required
		@ pause
		@ exit
	) else (
		@set INKSCAPE="%ProgramFiles(x86)%\inkscape\inkscape.exe"
	)
) else (
	@set INKSCAPE="%ProgramFiles%\inkscape\inkscape.exe"
)
@echo Inkscape is %INKSCAPE%  

@if not exist pngcrush.exe (
	@ echo pngcrush from http://sourceforge.net/projects/pmt/files/ is required
	@ pause
	@ exit
)

cd main
call :svg2png 24 small_
call :svg2png 32
call :svg2png 48 big_
call :svg2png 96 large_
call :svg2png 192 huge_
cd..

cd rec
call :svg2png 24 small_
call :svg2png 32
call :svg2png 48 big_
call :svg2png 96 large_
call :svg2png 192 huge_
cd..

cd route
call :svg2png 24 small_
call :svg2png 32
call :svg2png 48 big_
call :svg2png 96 large_
call :svg2png 192 huge_
cd..

cd osm
call :svg2png 24 small_
call :svg2png 32
call :svg2png 48 big_
call :svg2png 96 large_
call :svg2png 192 huge_
cd..

cd setup
call :svg2png 24 small_
call :svg2png 32
call :svg2png 48 big_
call :svg2png 96 large_
call :svg2png 192 huge_
cd..


pause
exit

REM %1: edgeLen of icon  Parameter %2: prefix for png
:svg2png
REM Konvertieren der svg-Dateien im aktuellen Verzeichnis nach png
for /R %%i in (*.svg) do start "Inkscape" /wait %INKSCAPE% -f "%%~fi" -e "%%~dpi%2%%~ni.png" -D -w %1

REM Komprimieren der png-Dateien in den Unterordner crushed
..\pngcrush >nul -d crushed -rem allb -brute -reduce %2*.png
REM Ersetzen der png-Dateien durch die komprimierten aus dem Unterordner crushed
copy crushed\*.png .
REM Löschen des Unterordners crushed
rmdir crushed /Q /S
goto :eof
