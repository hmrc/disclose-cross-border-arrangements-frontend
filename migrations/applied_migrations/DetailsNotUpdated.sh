#!/bin/bash

echo ""
echo "Applying migration DetailsNotUpdated"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /detailsNotUpdated                       controllers.DetailsNotUpdatedController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "detailsNotUpdated.title = detailsNotUpdated" >> ../conf/messages.en
echo "detailsNotUpdated.heading = detailsNotUpdated" >> ../conf/messages.en

echo "Migration DetailsNotUpdated completed"
