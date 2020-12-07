#!/bin/bash

echo ""
echo "Applying migration ContactTelephoneNumber"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /contactTelephoneNumber                        controllers.ContactTelephoneNumberController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /contactTelephoneNumber                        controllers.ContactTelephoneNumberController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeContactTelephoneNumber                  controllers.ContactTelephoneNumberController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeContactTelephoneNumber                  controllers.ContactTelephoneNumberController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "contactTelephoneNumber.title = contactTelephoneNumber" >> ../conf/messages.en
echo "contactTelephoneNumber.heading = contactTelephoneNumber" >> ../conf/messages.en
echo "contactTelephoneNumber.checkYourAnswersLabel = contactTelephoneNumber" >> ../conf/messages.en
echo "contactTelephoneNumber.error.required = Enter contactTelephoneNumber" >> ../conf/messages.en
echo "contactTelephoneNumber.error.length = ContactTelephoneNumber must be 24 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContactTelephoneNumberUserAnswersEntry: Arbitrary[(ContactTelephoneNumberPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[ContactTelephoneNumberPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContactTelephoneNumberPage: Arbitrary[ContactTelephoneNumberPage.type] =";\
    print "    Arbitrary(ContactTelephoneNumberPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(ContactTelephoneNumberPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def contactTelephoneNumber: Option[Row] = userAnswers.get(ContactTelephoneNumberPage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"contactTelephoneNumber.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(lit\"$answer\"),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.ContactTelephoneNumberController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"contactTelephoneNumber.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration ContactTelephoneNumber completed"
