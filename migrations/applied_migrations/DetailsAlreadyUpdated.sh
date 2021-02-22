#!/bin/bash

echo ""
echo "Applying migration DetailsAlreadyUpdated"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /detailsAlreadyUpdated                       controllers.DetailsAlreadyUpdatedController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "detailsAlreadyUpdated.title = detailsAlreadyUpdated" >> ../conf/messages.en
echo "detailsAlreadyUpdated.heading = detailsAlreadyUpdated" >> ../conf/messages.en

echo "Migration DetailsAlreadyUpdated completed"
