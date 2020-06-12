#!/bin/bash

echo ""
echo "Applying migration VirusError"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /virusError                       controllers.VirusErrorController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "virusError.title = virusError" >> ../conf/messages.en
echo "virusError.heading = virusError" >> ../conf/messages.en

echo "Migration VirusError completed"
