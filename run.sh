#!/usr/bin/env sh
# Compiles and launches the game with only a JDK (no Maven needed).
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac --release 17 -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/. out/
java -cp out chess.ChessApp
