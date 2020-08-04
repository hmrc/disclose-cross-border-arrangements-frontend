#!/bin/bash

echo ""
echo "Applying migration DeleteDisclosure"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /deleteDisclosure                       controllers.DeleteDisclosureController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteDisclosure.title = deleteDisclosure" >> ../conf/messages.en
echo "deleteDisclosure.heading = deleteDisclosure" >> ../conf/messages.en

echo "Migration DeleteDisclosure completed"
