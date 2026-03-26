#!/bin/sh
# Maven wrapper - downloads and runs Maven
MAVEN_VERSION="3.9.5"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -d "$MAVEN_HOME" ]; then
    mkdir -p "$MAVEN_HOME"
    curl -s "$MAVEN_URL" | tar xz -C "$HOME/.m2/wrapper/dists/"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
