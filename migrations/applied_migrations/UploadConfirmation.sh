#!/bin/bash

echo ""
echo "Applying migration UploadConfirmation"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /uploadConfirmation                       controllers.UploadConfirmationController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "uploadConfirmation.title = uploadConfirmation" >> ../conf/messages.en
echo "uploadConfirmation.heading = uploadConfirmation" >> ../conf/messages.en

echo "Migration UploadConfirmation completed"
