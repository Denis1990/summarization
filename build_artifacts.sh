#!/bin/bash

if [[ ! -d "dist/" ]]; then
    mkdir 'dist'
fi

mvn -Pdemo -DskipTests=true clean package
cp target/summarization.jar dist/demoLucene.jar

mvn -Pextractor -DskipTests=true clean package
cp target/summarization.jar dist/extractorDemo.jar

mvn -Prelease clean package
cp target/summarization.jar dist/

cp -r target/dependency dist/

if [[ ! -d "$HOME/Documents/demoLucene" ]]; then
    echo "[WARNING] $HOME/Documents/demoLucene directory does not exist!!!"
    echo "[WARNING] Create directory and populate it with documents in order to run the demo."
fi

