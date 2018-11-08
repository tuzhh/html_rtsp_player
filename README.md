## Overview

html_rtsp_player is a Javascript library which implements RTSP client for watching live streams in your browser 
that works directly on top of a standard HTML <video> element. 
It requires support of HTML5 Video with Media Sources Extensions for playback. Also player relies on server-side websocket 
proxy for retransmitting RTSP streams to browser.

## Test

Running server html_rtsp_player and test page http://localhost:9000/

## Browser support

* Firefox v.42+
* Chrome v.23+
* OSX Safari v.8+
* MS Edge v.13+
* Opera v.15+
* Android browser v.5.0+
* IE Mobile v.11+


## Prerequisite

jdk 1.8

maven 3.3.9

## Install

git clone https://github.com/tuzhh/html_rtsp_player.git

cd html_rtsp_player

mvn clean compile install

## Usage

### Server side

java -jar target/html-rtsp-player-0.0.1-SNAPSHOT.jar

### Browser side

http://localhost:9000/

Have any suggestions for improving work of our player? 

Feel free to leave comments or ideas to tuzhihai@gmail.com
