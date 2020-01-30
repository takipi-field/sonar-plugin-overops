# OverOps SonarQube Plugin

Building the plugin

1. Clone this repo and in the root directory run ```mvn clean package```
2. Go into the `/target` and copy the ```overops-1.0.0.jar``` into the sonar server ```extensions/plugins``` directory
3. Restart the server and plugin will auto install onto it.
