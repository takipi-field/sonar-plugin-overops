SONAR_URL=http://localhost:9000

OVEROPS_APP_URL=http://localhost:8080
OVEROPS_API_URL=http://localhost:8080
OVEROPS_ENVIRONMENT=S1

# Build Plugin
# mvn clean install

# Spin up Sonar with Plugin and a Collector
docker-compose up -d

# Configure Sonar with OverOps Plugin
echo ""
printf "Waiting for Sonar to start "
until $(curl --output /dev/null --head --fail --silent -u admin:admin "$SONAR_URL/api/system/health"); do
    printf '.'
    sleep 5
done

echo ""
echo ""

# Configure OverOps Plugin Variables
curl -u admin:admin --request POST "$SONAR_URL/api/settings/set?key=overops.app.url&value=$OVEROPS_APP_URL"
curl -u admin:admin --request POST "$SONAR_URL/api/settings/set?key=overops.api.url&value=$OVEROPS_API_URL"
curl -u admin:admin --request POST "$SONAR_URL/api/settings/set?key=overops.environment.id&value=$OVEROPS_ENVIRONMENT"

# CS
echo "Settting Up CS Quality Profile"

echo "- Fetch Sonar Way CS Profile ID"
SONAR_WAY_CS_ID=$(curl -s -u admin:admin --request GET "$SONAR_URL/api/qualityprofiles/search?qualityProfile=Sonar+way&language=cs" \
 | sed -n 's|.*"key":"\([^"]*\)".*|\1|p')

echo "- Copy Sonar Way Profile for CS"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/qualityprofiles/copy?fromKey=$SONAR_WAY_CS_ID&toName=OverOpsCS"

echo "- Fetch OverOps CS Profile ID"
OVEROPS_CS_ID=$(curl -s -u admin:admin --request GET "$SONAR_URL/api/qualityprofiles/search?qualityProfile=OverOpsCS" \
 | sed -n 's|.*"key":"\([^"]*\)".*|\1|p')

echo "- Add OverOps Event CS Rule"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/qualityprofiles/activate_rules?targetKey=$OVEROPS_CS_ID&rule_key=overops-dotnet:event"
echo ""

# JAVA
echo "Settting Up Java Quality Profile"
echo "- Fetch Sonar Way Java Profile ID"
SONAR_WAY_JAVA_ID=$(curl -s -u admin:admin --request GET "$SONAR_URL/api/qualityprofiles/search?qualityProfile=Sonar+way&language=java" \
 | sed -n 's|.*"key":"\([^"]*\)".*|\1|p')

echo "- Copy Sonar Way Profile for Java"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/qualityprofiles/copy?fromKey=$SONAR_WAY_JAVA_ID&toName=OverOpsJava"

echo "- Fetch OverOps Java Profile ID"
OVEROPS_JAVA_ID=$(curl -s -u admin:admin --request GET "$SONAR_URL/api/qualityprofiles/search?qualityProfile=OverOpsJava" \
 | sed -n 's|.*"key":"\([^"]*\)".*|\1|p')

echo "- Add OverOps Event Java Rule"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/qualityprofiles/activate_rules?targetKey=$OVEROPS_JAVA_ID&rule_key=overops:event"
echo ""

# Create New CS Project
echo "Create Event Generator CS Project"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/projects/create?name=EventGeneratorCS&project=EventGeneratorCS"

echo "- Add OverOpsCS Quality Profile to EventGeneratorCS Project"
curl -s -u admin:admin --request POST "$SONAR_URL/api/qualityprofiles/add_project?language=cs&project=EventGeneratorCS&qualityProfile=OverOpsCS"

echo "- Set Application / Deployment for EventGeneratorCS"
curl -s -u admin:admin --request POST "$SONAR_URL/api/settings/set?component=EventGeneratorCS&key=overops.application.name&value=EventGeneratorCS"
curl -s -u admin:admin --request POST "$SONAR_URL/api/settings/set?component=EventGeneratorCS&key=overops.deployment.name&value=deployment1"
echo ""

# Create New Java Project
echo "Create Event Generator Java Project"
curl -s -u admin:admin --output /dev/null --request POST "$SONAR_URL/api/projects/create?name=EventGeneratorJava&project=EventGeneratorJava"

echo "- Add OverOpsJava Quality Profile to EventGeneratorJava Project"
curl -s -u admin:admin --request POST "$SONAR_URL/api/qualityprofiles/add_project?language=java&project=EventGeneratorJava&qualityProfile=OverOpsJava"

echo "- Set Application / Deployment for EventGeneratorJava"
curl -s -u admin:admin --request POST "$SONAR_URL/api/settings/set?component=EventGeneratorJava&key=overops.application.name&value=EventGeneratorJava"
curl -s -u admin:admin --request POST "$SONAR_URL/api/settings/set?component=EventGeneratorJava&key=overops.deployment.name&value=deployment1"
echo ""

ACCESS_TOKEN=$(curl -s -u admin:admin --request POST "$SONAR_URL/api/user_tokens/generate?name=Test" \
 | sed -n 's|.*"token":"\([^"]*\)".*|\1|p')

echo "Sonar Server Started and Configured for Testing"
echo "***************************************************************************"
echo "Access Token: $ACCESS_TOKEN"
echo "              (Use this in the Sonar Maven Commmand)"
echo "***************************************************************************"
echo ""
