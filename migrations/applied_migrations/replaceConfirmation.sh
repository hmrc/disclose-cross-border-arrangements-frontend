#!/bin/bash

echo ""
echo "Applying migration replaceConfirmation"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /replaceConfirmation                       controllers.replaceConfirmationController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "replaceConfirmation.title = replaceConfirmation" >> ../conf/messages.en
echo "replaceConfirmation.heading = replaceConfirmation" >> ../conf/messages.en

echo "Migration replaceConfirmation completed"
