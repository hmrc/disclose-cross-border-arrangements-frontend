#!/bin/bash

echo ""
echo "Applying migration FileError"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /fileError                       controllers.FileErrorController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "fileError.title = fileError" >> ../conf/messages.en
echo "fileError.heading = fileError" >> ../conf/messages.en

echo "Migration FileError completed"
