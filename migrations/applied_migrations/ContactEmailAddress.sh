#!/bin/bash

echo ""
echo "Applying migration ContactEmailAddress"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /contactEmailAddress                        controllers.ContactEmailAddressController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /contactEmailAddress                        controllers.ContactEmailAddressController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeContactEmailAddress                  controllers.ContactEmailAddressController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeContactEmailAddress                  controllers.ContactEmailAddressController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "contactEmailAddress.title = contactEmailAddress" >> ../conf/messages.en
echo "contactEmailAddress.heading = contactEmailAddress" >> ../conf/messages.en
echo "contactEmailAddress.checkYourAnswersLabel = contactEmailAddress" >> ../conf/messages.en
echo "contactEmailAddress.error.required = Enter contactEmailAddress" >> ../conf/messages.en
echo "contactEmailAddress.error.length = ContactEmailAddress must be 132 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContactEmailAddressUserAnswersEntry: Arbitrary[(ContactEmailAddressPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[ContactEmailAddressPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContactEmailAddressPage: Arbitrary[ContactEmailAddressPage.type] =";\
    print "    Arbitrary(ContactEmailAddressPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(ContactEmailAddressPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def contactEmailAddress: Option[Row] = userAnswers.get(ContactEmailAddressPage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"contactEmailAddress.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(lit\"$answer\"),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.ContactEmailAddressController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"contactEmailAddress.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration ContactEmailAddress completed"
