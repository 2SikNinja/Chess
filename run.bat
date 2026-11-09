@echo off
rem Compiles and launches the game with only a JDK (no Maven needed).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -d out @sources.txt
if errorlevel 1 (
    del sources.txt
    exit /b 1
)
del sources.txt
xcopy /e /i /q src\main\resources out > nul
java -cp out chess.ChessApp
