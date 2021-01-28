#!/bin/bash

echo ""
echo "Applying migration ContactUsToUseManualService"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /contactUsToUseManualService                       controllers.ContactUsToUseManualServiceController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "contactUsToUseManualService.title = contactUsToUseManualService" >> ../conf/messages.en
echo "contactUsToUseManualService.heading = contactUsToUseManualService" >> ../conf/messages.en

echo "Migration ContactUsToUseManualService completed"
