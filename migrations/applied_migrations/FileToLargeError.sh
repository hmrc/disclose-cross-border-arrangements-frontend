#!/bin/bash

echo ""
echo "Applying migration FileToLargeError"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /fileToLargeError                       controllers.FileToLargeErrorController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "fileToLargeError.title = fileToLargeError" >> ../conf/messages.en
echo "fileToLargeError.heading = fileToLargeError" >> ../conf/messages.en

echo "Migration FileToLargeError completed"
