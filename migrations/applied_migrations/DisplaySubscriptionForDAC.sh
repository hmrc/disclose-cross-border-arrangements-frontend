#!/bin/bash

echo ""
echo "Applying migration DisplaySubscriptionForDAC"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /displaySubscriptionForDAC                       controllers.DisplaySubscriptionForDACController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "displaySubscriptionForDAC.title = displaySubscriptionForDAC" >> ../conf/messages.en
echo "displaySubscriptionForDAC.heading = displaySubscriptionForDAC" >> ../conf/messages.en

echo "Migration DisplaySubscriptionForDAC completed"
