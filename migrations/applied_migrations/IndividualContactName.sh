#!/bin/bash

echo ""
echo "Applying migration IndividualContactName"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /individualContactName                        controllers.IndividualContactNameController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /individualContactName                        controllers.IndividualContactNameController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeIndividualContactName                  controllers.IndividualContactNameController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeIndividualContactName                  controllers.IndividualContactNameController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "individualContactName.title = individualContactName" >> ../conf/messages.en
echo "individualContactName.heading = individualContactName" >> ../conf/messages.en
echo "individualContactName.firstName = firstName" >> ../conf/messages.en
echo "individualContactName.lastName = lastName" >> ../conf/messages.en
echo "individualContactName.checkYourAnswersLabel = individualContactName" >> ../conf/messages.en
echo "individualContactName.error.firstName.required = Enter firstName" >> ../conf/messages.en
echo "individualContactName.error.lastName.required = Enter lastName" >> ../conf/messages.en
echo "individualContactName.error.firstName.length = firstName must be 35 characters or less" >> ../conf/messages.en
echo "individualContactName.error.lastName.length = lastName must be 35 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryIndividualContactNameUserAnswersEntry: Arbitrary[(IndividualContactNamePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[IndividualContactNamePage.type]";\
    print "        value <- arbitrary[IndividualContactName].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryIndividualContactNamePage: Arbitrary[IndividualContactNamePage.type] =";\
    print "    Arbitrary(IndividualContactNamePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryIndividualContactName: Arbitrary[IndividualContactName] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        firstName <- arbitrary[String]";\
    print "        lastName <- arbitrary[String]";\
    print "      } yield IndividualContactName(firstName, lastName)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(IndividualContactNamePage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class CheckYourAnswersHelper/ {\
     print;\
     print "";\
     print "  def individualContactName: Option[Row] = userAnswers.get(IndividualContactNamePage) map {";\
     print "    answer =>";\
     print "      Row(";\
     print "        key     = Key(msg\"individualContactName.checkYourAnswersLabel\", classes = Seq(\"govuk-!-width-one-half\")),";\
     print "        value   = Value(lit\"${answer.firstName} ${answer.lastName}\"),";\
     print "        actions = List(";\
     print "          Action(";\
     print "            content            = msg\"site.edit\",";\
     print "            href               = routes.IndividualContactNameController.onPageLoad(CheckMode).url,";\
     print "            visuallyHiddenText = Some(msg\"site.edit.hidden\".withArgs(msg\"individualContactName.checkYourAnswersLabel\"))";\
     print "          )";\
     print "        )";\
     print "      )";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration IndividualContactName completed"
