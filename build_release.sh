#!/bin/bash
export JAVA_HOME=/home/linuxbrew/.linuxbrew/Cellar/openjdk@21/21.0.11
cd /home/trappy/projects/zx_buddyv1
./gradlew --no-configuration-cache assembleRelease 2>&1 | tail -100
