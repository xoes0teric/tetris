#!/usr/bin/env bash
# Compile and run the JUnit tests
set -e
cd "$(dirname "$0")"

JUNIT=lib/junit-platform-console-standalone-1.11.4.jar

rm -rf bin-test
javac -cp "$JUNIT" -d bin-test $(find src tests -name "*.java")
java -jar "$JUNIT" execute --class-path bin-test --scan-class-path
