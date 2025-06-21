#!/bin/bash

# Build and run the RuneLite plugin with macOS compatibility flags

echo "Building plugin..."
./gradlew shadowJar

if [ $? -eq 0 ]; then
    echo "Launching RuneLite with plugin..."
    java -ea --add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED -jar build/libs/runelite-skill-unlocks-1.0-SNAPSHOT-all.jar
else
    echo "Build failed!"
    exit 1
fi