@if not exist %ProgramFiles%\inkscape\inkscape.exe (
@ echo Inkscape from http://www.inkscape.org/download/ is required
@ pause
@ exit
)

@if not exist pngcrush.exe (
@ echo pngcrush from http://sourceforge.net/projects/pmt/files/ is required
@ pause
@ exit
)

cd main
call :svg2png
cd..

cd rec
call :svg2png
cd..

cd route
call :svg2png
cd..

cd osm
call :svg2png
cd..

cd setup
call :svg2png
cd..


pause
exit

:svg2png
REM Konvertieren der svg-Dateien im aktuellen Verzeichnis nach png
for /R %%i in (*.svg) do start /wait %ProgramFiles%\inkscape\inkscape.exe -f "%%~fi" -e "%%~dpni.png" -D -w 24

REM Komprimieren der png-Dateien in den Unterordner crushed
..\pngcrush -d crushed -brute *.png
REM Ersetzen der png-Dateien durch die komprimierten aus dem Unterordner crushed
copy crushed\*.png .
REM Löschen des Unterordners crushed
rmdir crushed /Q /S
goto :eof
