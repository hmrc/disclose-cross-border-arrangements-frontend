#!/bin/bash

echo ""
echo "Applying migration SecondaryContactEmailAddress"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /secondaryContactEmailAddress                        controllers.SecondaryContactEmailAddressController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /secondaryContactEmailAddress                        controllers.SecondaryContactEmailAddressController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeSecondaryContactEmailAddress                  controllers.SecondaryContactEmailAddressController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeSecondaryContactEmailAddress                  controllers.SecondaryContactEmailAddressController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "secondaryContactEmailAddress.title = secondaryContactEmailAddress" >> ../conf/messages.en
echo "secondaryContactEmailAddress.heading = secondaryContactEmailAddress" >> ../conf/messages.en
echo "secondaryContactEmailAddress.checkYourAnswersLabel = secondaryContactEmailAddress" >> ../conf/messages.en
echo "secondaryContactEmailAddress.error.required = Enter secondaryContactEmailAddress" >> ../conf/messages.en
echo "secondaryContactEmailAddress.error.length = SecondaryContactEmailAddress must be 132 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySecondaryContactEmailAddressUserAnswersEntry: Arbitrary[(SecondaryContactEmailAddressPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SecondaryContactEmailAddressPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySecondaryContactEmailAddressPage: Arbitrary[SecondaryContactEmailAddressPage.type] =";\
    print "    Arbitrary(SecondaryContactEmailAddressPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SecondaryContactEmailAddressPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def secondaryContactEmailAddress: Option[Row] = userAnswers.get(SecondaryContactEmailAddressPage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"secondaryContactEmailAddress.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(lit\"$answer\"),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.SecondaryContactEmailAddressController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"secondaryContactEmailAddress.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration SecondaryContactEmailAddress completed"
