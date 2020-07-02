mvn clean install

docker build -t overops/sonar .

docker run -it --rm -p 9000:9000 --name overops-sonar overops/sonar

