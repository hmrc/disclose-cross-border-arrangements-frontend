/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package navigation

import base.SpecBase
import controllers.routes
import generators.Generators
import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages._
import pages.contactdetails.{ContactEmailAddressPage, ContactNamePage, ContactTelephoneNumberPage, IndividualContactNamePage, SecondaryContactEmailAddressPage, SecondaryContactNamePage, SecondaryContactTelephoneNumberPage}

class NavigatorSpec extends SpecBase with ScalaCheckPropertyChecks with Generators {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page

        forAll(arbitrary[UserAnswers]) {
          answers =>

            navigator.nextPage(UnknownPage, NormalMode, answers)
              .mustBe(routes.IndexController.onPageLoad())
        }
      }

      "must go from file validation page to 'Your file does not contain valid XML' page if XML is invalid" in {

        forAll(arbitrary[UserAnswers]) {
          answers =>
            val updatedAnswers =
              answers
                .set(InvalidXMLPage, "fileName.xml")
                .success
                .value
            navigator.nextPage(InvalidXMLPage, NormalMode, updatedAnswers)
              .mustBe(routes.InvalidXMLController.onPageLoad())
        }
      }


      "must go from file validation page to 'Check your answer before sending file' page if XML is Valid" in {

        forAll(arbitrary[UserAnswers]) {
          answers =>
            val updatedAnswers =
              answers
                .set(ValidXMLPage, "fileName.xml")
                .success
                .value
            navigator.nextPage(ValidXMLPage, NormalMode, updatedAnswers)
              .mustBe(routes.CheckYourAnswersController.onPageLoad())
        }
      }

      "must go from history page to '/your-disclosures-results' page" in {

        forAll(arbitrary[UserAnswers]) {
          answers =>
            val updatedAnswers =
              answers
                .set(HistoryPage, "fileName.xml")
                .success
                .value
            navigator.nextPage(HistoryPage, NormalMode, updatedAnswers)
              .mustBe(routes.SearchHistoryResultsController.onPageLoad())
        }
      }

      "must go from What is the name of the individual or team we should contact? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validPersonalName, validPersonalName) {
          (answers, firstName, lastName) =>
            val updatedAnswers =
              answers
                .set(ContactNamePage, Name(firstName, lastName))
                .success
                .value
            navigator.nextPage(ContactNamePage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from Who should we contact if we have any questions about your disclosures? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validPersonalName, validPersonalName) {
          (answers, firstName, lastName) =>
            val updatedAnswers =
              answers
                .set(IndividualContactNamePage, Name(firstName, lastName))
                .success
                .value
            navigator.nextPage(IndividualContactNamePage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from What is your email address? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validEmailAddress) {
          (answers, email) =>
            val updatedAnswers =
              answers
                .set(ContactEmailAddressPage, email)
                .success
                .value
            navigator.nextPage(ContactEmailAddressPage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from What is your telephone number? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validPhoneNumber) {
          (answers, phone) =>
            val updatedAnswers =
              answers
                .set(ContactTelephoneNumberPage, phone)
                .success
                .value
            navigator.nextPage(ContactTelephoneNumberPage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from What is the name of the individual or team we should contact? page to " +
        "Check your contact details page - Secondary contact" in {

        forAll(arbitrary[UserAnswers], validOrganisationName) {
          (answers, orgName) =>
            val updatedAnswers =
              answers
                .set(SecondaryContactNamePage, orgName)
                .success
                .value
            navigator.nextPage(SecondaryContactNamePage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from What is the email address for your second contact? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validEmailAddress) {
          (answers, email) =>
            val updatedAnswers =
              answers
                .set(SecondaryContactEmailAddressPage, email)
                .success
                .value
            navigator.nextPage(SecondaryContactEmailAddressPage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }

      "must go from What is the telephone number for your second contact? page to Check your contact details page" in {

        forAll(arbitrary[UserAnswers], validPhoneNumber) {
          (answers, phone) =>
            val updatedAnswers =
              answers
                .set(SecondaryContactTelephoneNumberPage, phone)
                .success
                .value
            navigator.nextPage(SecondaryContactTelephoneNumberPage, NormalMode, updatedAnswers)
              .mustBe(routes.ContactDetailsController.onPageLoad())
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map  to Check Your Answers" in {

        case object UnknownPage extends Page

        forAll(arbitrary[UserAnswers]) {
          answers =>

            navigator.nextPage(UnknownPage, CheckMode, answers)
              .mustBe(routes.CheckYourAnswersController.onPageLoad())
        }
      }
    }
  }
}
