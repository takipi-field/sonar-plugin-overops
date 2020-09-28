FROM sonarqube:8.2-community

COPY target/overops-plugin-1.0.6.jar /opt/sonarqube/extensions/plugins/
