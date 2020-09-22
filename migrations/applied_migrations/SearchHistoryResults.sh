#!/bin/bash

echo ""
echo "Applying migration SearchHistoryResults"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /searchHistoryResults                       controllers.SearchHistoryResultsController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "searchHistoryResults.title = searchHistoryResults" >> ../conf/messages.en
echo "searchHistoryResults.heading = searchHistoryResults" >> ../conf/messages.en

echo "Migration SearchHistoryResults completed"
