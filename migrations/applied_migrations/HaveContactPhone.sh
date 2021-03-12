#!/bin/bash

echo ""
echo "Applying migration HaveContactPhone"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /haveContactPhone                        controllers.HaveContactPhoneController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /haveContactPhone                        controllers.HaveContactPhoneController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeHaveContactPhone                  controllers.HaveContactPhoneController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeHaveContactPhone                  controllers.HaveContactPhoneController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "haveContactPhone.title = haveContactPhone" >> ../conf/messages.en
echo "haveContactPhone.heading = haveContactPhone" >> ../conf/messages.en
echo "haveContactPhone.checkYourAnswersLabel = haveContactPhone" >> ../conf/messages.en
echo "haveContactPhone.error.required = Select yes if haveContactPhone" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryHaveContactPhoneUserAnswersEntry: Arbitrary[(HaveContactPhonePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[HaveContactPhonePage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryHaveContactPhonePage: Arbitrary[HaveContactPhonePage.type] =";\
    print "    Arbitrary(HaveContactPhonePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(HaveContactPhonePage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def haveContactPhone: Option[Row] = userAnswers.get(HaveContactPhonePage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"haveContactPhone.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(yesOrNo(answer)),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.HaveContactPhoneController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"haveContactPhone.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration HaveContactPhone completed"
