#!/bin/bash

echo ""
echo "Applying migration ContactDetails"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /contactDetails                       controllers.ContactDetailsController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "contactDetails.title = contactDetails" >> ../conf/messages.en
echo "contactDetails.heading = contactDetails" >> ../conf/messages.en

echo "Migration ContactDetails completed"
