#!/bin/bash

mode=$1

if [[ $mode == 'redeploy' ]]
then
  echo "Redeploying..."
  asadmin --user admin --host cad0 --port 4848 redeploy --name easyhome --dropandcreatetables=true target/easyhome-rest-0.0.1-SNAPSHOT.war
else
  echo "Deploying..."
  asadmin --user admin --host cad0 --port 4848 deploy --name easyhome --contextroot /easyhome --dropandcreatetables=true target/easyhome-rest-0.0.1-SNAPSHOT.war
fi
