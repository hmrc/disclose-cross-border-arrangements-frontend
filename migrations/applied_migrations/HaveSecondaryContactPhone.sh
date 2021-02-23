#!/bin/bash

echo ""
echo "Applying migration HaveSecondaryContactPhone"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /haveSecondaryContactPhone                        controllers.HaveSecondaryContactPhoneController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /haveSecondaryContactPhone                        controllers.HaveSecondaryContactPhoneController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeHaveSecondaryContactPhone                  controllers.HaveSecondaryContactPhoneController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeHaveSecondaryContactPhone                  controllers.HaveSecondaryContactPhoneController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "haveSecondaryContactPhone.title = haveSecondaryContactPhone" >> ../conf/messages.en
echo "haveSecondaryContactPhone.heading = haveSecondaryContactPhone" >> ../conf/messages.en
echo "haveSecondaryContactPhone.checkYourAnswersLabel = haveSecondaryContactPhone" >> ../conf/messages.en
echo "haveSecondaryContactPhone.error.required = Select yes if haveSecondaryContactPhone" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryHaveSecondaryContactPhoneUserAnswersEntry: Arbitrary[(HaveSecondaryContactPhonePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[HaveSecondaryContactPhonePage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryHaveSecondaryContactPhonePage: Arbitrary[HaveSecondaryContactPhonePage.type] =";\
    print "    Arbitrary(HaveSecondaryContactPhonePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(HaveSecondaryContactPhonePage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def haveSecondaryContactPhone: Option[Row] = userAnswers.get(HaveSecondaryContactPhonePage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"haveSecondaryContactPhone.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(yesOrNo(answer)),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.HaveSecondaryContactPhoneController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"haveSecondaryContactPhone.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration HaveSecondaryContactPhone completed"
