@echo off
cd /d "%~dp0"
copy /b "YouTube-V58-native1.apk.part1"+"YouTube-V58-native1.apk.part2"+"YouTube-V58-native1.apk.part3" "YouTube-V58-native1.apk"
if errorlevel 1 exit /b 1
certutil -hashfile "YouTube-V58-native1.apk" SHA256
echo Compare this SHA256 with artifact.json before installing.
pause
