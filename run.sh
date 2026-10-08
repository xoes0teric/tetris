#!/usr/bin/env bash
# Compile and run Tetris
set -e
cd "$(dirname "$0")"

rm -rf bin
javac -d bin $(find src -name "*.java")
java -cp bin game.Main
