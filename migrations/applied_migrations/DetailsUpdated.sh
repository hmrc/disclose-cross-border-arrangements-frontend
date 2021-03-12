#!/bin/bash

echo ""
echo "Applying migration DetailsUpdated"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /detailsUpdated                       controllers.DetailsUpdatedController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "detailsUpdated.title = detailsUpdated" >> ../conf/messages.en
echo "detailsUpdated.heading = detailsUpdated" >> ../conf/messages.en

echo "Migration DetailsUpdated completed"
