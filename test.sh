#!/usr/bin/env bash
# Compile and run the tests
set -e
cd "$(dirname "$0")"

rm -rf test-bin
javac -d test-bin $(find src test -name "*.java")
java -Djava.awt.headless=true -cp test-bin tests.TestRunner
