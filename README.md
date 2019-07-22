SonarQube Custom Plugin Example [![Build Status](https://travis-ci.org/SonarSource/sonar-custom-plugin-example.svg)](https://travis-ci.org/SonarSource/sonar-custom-plugin-example)
==========

Shows how to write a SonarQube plugin compatible with SonarQube 6.7 LTS

Custom Pages (React)
====================

Prerequisites
-------------

* [NodeJS](https://nodejs.org/en/)

Scripts
-------

* run "npm install" to setup your env
* run "npm test" to trigger your tests
* run "npm start" to start a proxy http server on port 3000 to debug your JS code

How to use this plugin. 
1. Clone this repo and in the root directory run ```mvn clean package```
1. Go into the `/target` and copy the ```sonar-overops-plugin-6.7.1.jar``` into the sonar server ```extensions/plugins``` directory
1. Restart the server and plugin will auto install onto it.