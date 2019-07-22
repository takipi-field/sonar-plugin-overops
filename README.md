SonarQube Custom Plugin OverOps [![Build Status](https://travis-ci.org/SonarSource/sonar-custom-plugin-example.svg)](https://travis-ci.org/SonarSource/sonar-custom-plugin-example)
==========

How to use this plugin. 
1. Clone this repo and in the root directory run ```mvn clean package```
1. Go into the `/target` and copy the ```sonar-overops-plugin-6.7.1.jar``` into the sonar server ```extensions/plugins``` directory
1. Restart the server and plugin will auto install onto it.