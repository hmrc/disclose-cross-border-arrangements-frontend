#!/bin/bash

echo ""
echo "Applying migration DeleteDisclosureConfirmation"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /deleteDisclosureConfirmation                       controllers.DeleteDisclosureConfirmationController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteDisclosureConfirmation.title = deleteDisclosureConfirmation" >> ../conf/messages.en
echo "deleteDisclosureConfirmation.heading = deleteDisclosureConfirmation" >> ../conf/messages.en

echo "Migration DeleteDisclosureConfirmation completed"
