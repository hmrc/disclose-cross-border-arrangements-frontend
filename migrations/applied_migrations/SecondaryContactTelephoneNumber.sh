#!/bin/bash

echo ""
echo "Applying migration SecondaryContactTelephoneNumber"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /secondaryContactTelephoneNumber                        controllers.SecondaryContactTelephoneNumberController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /secondaryContactTelephoneNumber                        controllers.SecondaryContactTelephoneNumberController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeSecondaryContactTelephoneNumber                  controllers.SecondaryContactTelephoneNumberController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeSecondaryContactTelephoneNumber                  controllers.SecondaryContactTelephoneNumberController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "secondaryContactTelephoneNumber.title = secondaryContactTelephoneNumber" >> ../conf/messages.en
echo "secondaryContactTelephoneNumber.heading = secondaryContactTelephoneNumber" >> ../conf/messages.en
echo "secondaryContactTelephoneNumber.checkYourAnswersLabel = secondaryContactTelephoneNumber" >> ../conf/messages.en
echo "secondaryContactTelephoneNumber.error.required = Enter secondaryContactTelephoneNumber" >> ../conf/messages.en
echo "secondaryContactTelephoneNumber.error.length = SecondaryContactTelephoneNumber must be 24 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySecondaryContactTelephoneNumberUserAnswersEntry: Arbitrary[(SecondaryContactTelephoneNumberPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SecondaryContactTelephoneNumberPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySecondaryContactTelephoneNumberPage: Arbitrary[SecondaryContactTelephoneNumberPage.type] =";\
    print "    Arbitrary(SecondaryContactTelephoneNumberPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SecondaryContactTelephoneNumberPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def secondaryContactTelephoneNumber: Option[Row] = userAnswers.get(SecondaryContactTelephoneNumberPage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"secondaryContactTelephoneNumber.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(lit\"$answer\"),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.SecondaryContactTelephoneNumberController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"secondaryContactTelephoneNumber.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration SecondaryContactTelephoneNumber completed"
