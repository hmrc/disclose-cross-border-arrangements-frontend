#!/bin/bash

echo ""
echo "Applying migration CreateConfirmation"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /createConfirmation                       controllers.CreateConfirmationController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "createConfirmation.title = createConfirmation" >> ../conf/messages.en
echo "createConfirmation.heading = createConfirmation" >> ../conf/messages.en

echo "Migration CreateConfirmation completed"
