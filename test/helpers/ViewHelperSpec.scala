/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers

import base.SpecBase
import generators.Generators
import helpers.JsonFixtures.{displaySubscriptionPayload, displaySubscriptionPayloadNoSecondary}
import models.subscription._
import models.{GenericError, SubmissionDetails, SubmissionHistory, UserAnswers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import pages.DisplaySubscriptionDetailsPage
import pages.contactdetails.{ContactNamePage, HaveContactPhonePage, HaveSecondaryContactPhonePage, SecondaryContactNamePage}
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, Table, _}

import java.time.LocalDateTime

class ViewHelperSpec extends SpecBase with Generators {

  val viewHelper       = new ViewHelper
  val mockURL: JsValue = Json.toJson("www.test.com")

  val contactInformationForInd: ContactInformationForIndividual =
    ContactInformationForIndividual(individual = IndividualDetails(firstName = "FirstName", lastName = "LastName", middleName = None),
                                    email = "email@email.com",
                                    phone = Some("07111222333"),
                                    mobile = None
    )

  val contactInformationForOrg: ContactInformationForOrganisation =
    ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Organisation name"),
                                      email = "email@email.com",
                                      phone = Some("07111123333"),
                                      mobile = None
    )

  val jsonPayload: String = displaySubscriptionPayload(
    JsString("subscriptionID"),
    JsString("Organisation Name"),
    JsString("Secondary contact name"),
    JsString("email@email.com"),
    JsString("email2@email.com"),
    JsString("07111222333")
  )

  val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(jsonPayload).as[DisplaySubscriptionForDACResponse]

  "linkToHomePageText" - {

    "must return the correct go to home page content" in {

      viewHelper.linkToHomePageText(mockURL) mustBe Html(
        s"<a id='homepage-link' href=$mockURL class='govuk-link'>" +
          s"Disclose a cross-border arrangement</a>."
      )

    }
  }

  "surveyLinkText" - {

    "must return the correct beta feedback content" in {

      viewHelper.surveyLinkText(mockURL) mustBe Html(
        s"<a id='feedback-link' href=$mockURL class='govuk-link'>" +
          s"What did you think of this service?</a> (takes 30 seconds)"
      )

    }
  }

  "mapErrorsToTable" - {

    val head: Seq[Cell] = Seq(
      Cell(msg"invalidXML.table.heading1", classes = Seq("govuk-!-width-one-quarter", "govuk-table__header")),
      Cell(msg"invalidXML.table.heading2", classes = Seq("govuk-!-font-weight-bold"))
    )

    "must return a table containing a row with a line number & error when given a generic error" in {

      val mockSingleError: Seq[GenericError] = Seq(GenericError(11, "Enter your cats name in meow format"))

      viewHelper.mapErrorsToTable(mockSingleError) mustBe Table(
        head = head,
        rows = Seq(
          Seq(
            Cell(msg"11", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_11")),
            Cell(msg"Enter your cats name in meow format", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_11"))
          )
        ),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }

    "must return a table containing multiple rows with line numbers & errors when a given Generic Error" in {

      val mockMultiError: Seq[GenericError] =
        Seq(GenericError(22, "Enter cat years only"), GenericError(33, "Incorrect number of cat legs"))

      viewHelper.mapErrorsToTable(mockMultiError) mustBe Table(
        head = head,
        rows = Seq(
          Seq(
            Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_22")),
            Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_22"))
          ),
          Seq(
            Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_33")),
            Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_33"))
          )
        ),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }

    "must return a table containing multiple rows in correct order with line numbers & errors when a given Generic Error" in {

      val mockMultiError: Seq[GenericError] =
        Seq(GenericError(33, "Incorrect number of cat legs"), GenericError(48, "You gotta be kitten me"), GenericError(22, "Enter cat years only"))

      viewHelper.mapErrorsToTable(mockMultiError) mustBe Table(
        head = head,
        rows = Seq(
          Seq(
            Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_22")),
            Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_22"))
          ),
          Seq(
            Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_33")),
            Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_33"))
          ),
          Seq(
            Cell(msg"48", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_48")),
            Cell(msg"You gotta be kitten me", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_48"))
          )
        ),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }
  }

  "buildDisclosuresTable" - {
    "must return a table of the submission history" in {
      val head: Seq[Cell] = Seq(
        Cell(msg"submissionHistory.arn.label"),
        Cell(msg"submissionHistory.disclosureID.label"),
        Cell(msg"submissionHistory.submissionDate.label"),
        Cell(msg"submissionHistory.messageRef.label", classes = Seq("govuk-!-width-one-third")),
        Cell(msg"submissionHistory.disclosureType.label")
      )

      val mockSubmissionHistory = SubmissionHistory(
        List(
          SubmissionDetails(
            "enrolmentID",
            LocalDateTime.parse("2020-07-01T10:23:30"),
            "fileName",
            Some("arrangementID"),
            Some("disclosureID"),
            "New",
            initialDisclosureMA = false,
            messageRefId = "GBXADAC0001234567AAA00101"
          ),
          SubmissionDetails(
            "enrolmentID",
            LocalDateTime.parse("2020-07-02T20:23:30"),
            "fileName2",
            Some("arrangementID2"),
            Some("disclosureID2"),
            "Add",
            initialDisclosureMA = false,
            messageRefId = "GBXADAC0001234567AAA00102"
          )
        )
      )

      viewHelper.buildDisclosuresTable(mockSubmissionHistory) mustBe Table(
        head = head,
        rows = Seq(
          Seq(
            Cell(msg"arrangementID", attributes = Map("id" -> s"arrangementID_0")),
            Cell(msg"disclosureID", attributes = Map("id" -> s"disclosureID_0")),
            Cell(msg"10:23am on 1 July 2020", attributes = Map("id" -> s"submissionTime_0")),
            Cell(msg"GBXADAC0001234567AAA00101", attributes = Map("id" -> s"messageRef_0"), classes = Seq("govuk-!-width-one-third", "breakString")),
            Cell(msg"New", attributes = Map("id" -> s"disclosureType_0"))
          ),
          Seq(
            Cell(msg"arrangementID2", attributes = Map("id" -> s"arrangementID_1")),
            Cell(msg"disclosureID2", attributes = Map("id" -> s"disclosureID_1")),
            Cell(msg"8:23pm on 2 July 2020", attributes = Map("id" -> s"submissionTime_1")),
            Cell(msg"GBXADAC0001234567AAA00102", attributes = Map("id" -> s"messageRef_1"), classes = Seq("govuk-!-width-one-third", "breakString")),
            Cell(msg"Add", attributes = Map("id" -> s"disclosureType_1"))
          )
        ),
        attributes = Map("id" -> "disclosuresTable")
      )
    }
  }

  "buildDisplaySubscription" - {
    "must return the rows containing the subscription details" in {

      val primaryContact: PrimaryContact = PrimaryContact(
        Seq(
          ContactInformationForIndividual(
            individual = IndividualDetails(firstName = "John", lastName = "Business", middleName = None),
            email = "email@email.com",
            phone = Some("07111222333"),
            mobile = Some("07111222333")
          )
        )
      )

      val secondaryContact: SecondaryContact = SecondaryContact(
        Seq(
          ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Organisation Name"),
                                            email = "email2@email.com",
                                            phone = None,
                                            mobile = None
          )
        )
      )

      val responseDetail: ResponseDetail = ResponseDetail(subscriptionID = "XE0001234567890",
                                                          tradingName = Some("Trading Name"),
                                                          isGBUser = true,
                                                          primaryContact = primaryContact,
                                                          secondaryContact = Some(secondaryContact)
      )

      val expectedRows = Seq(
        Seq(
          Cell(msg"displaySubscriptionForDAC.subscriptionID", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"XE0001234567890", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "subscriptionID"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.tradingName", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"Trading Name", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "tradingName"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.isGBUser", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"true", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "isGBUser"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualContact", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"John Business", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "individualContact"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualEmail", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"email@email.com", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "individualEmail"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualPhone", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"07111222333", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "individualPhone"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualMobile", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"07111222333", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "individualMobile"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationContact", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"Organisation Name", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "organisationContact"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationEmail", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"email2@email.com", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "organisationEmail"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationPhone", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"None", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "organisationPhone"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationMobile", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"None", classes = Seq("govuk-!-width-one-third"), attributes = Map("id" -> "organisationMobile"))
        )
      )

      val result = viewHelper.buildDisplaySubscription(responseDetail, hasSecondContact = true)

      result mustBe Table(head = Seq(
                            Cell(msg"Information", classes = Seq("govuk-!-width-one-third")),
                            Cell(msg"Value", classes = Seq("govuk-!-width-one-third"))
                          ),
                          rows = expectedRows
      )
    }
  }

  "primaryContactPhoneExists" - {
    "must return value from HaveContactPhonePage if it exists" in {
      val userAnswers = UserAnswers(userAnswersId).set(HaveContactPhonePage, true).success.value
      val result      = viewHelper.primaryContactPhoneExists(Seq(), userAnswers)

      result mustBe true
    }

    "must infer the value using the contact information if HaveContactPhonePage hasn't been set" in {
      val result = viewHelper.primaryContactPhoneExists(Seq(contactInformationForOrg), emptyUserAnswers)

      result mustBe true
    }
  }

  "secondaryContactPhoneExists" - {
    "must return value from HaveSecondaryContactPhonePage if it exists" in {
      val userAnswers = UserAnswers(userAnswersId).set(HaveSecondaryContactPhonePage, true).success.value
      val result      = viewHelper.secondaryContactPhoneExists(Seq(), userAnswers)

      result mustBe true
    }

    "must infer the value using the contact information if HaveSecondaryContactPhonePage hasn't been set" in {
      val result = viewHelper.secondaryContactPhoneExists(Seq(), emptyUserAnswers)

      result mustBe false
    }
  }

  "isOrganisation" - {
    "must return false if contact is an Individual" in {
      val result = viewHelper.isOrganisation(Seq(contactInformationForInd))

      result mustBe false
    }

    "must return true if contact is an Organisation" in {
      val result = viewHelper.isOrganisation(Seq(contactInformationForOrg))

      result mustBe true
    }
  }

  "retrieveContactName" - {
    "must return the individual name from contact details and return false if not an organisation" in {
      val result = viewHelper.retrieveContactName(Seq(contactInformationForInd))

      result mustBe "FirstName LastName"
    }

    "must return the organisation name from contact details and return true if an organisation" in {
      val result = viewHelper.retrieveContactName(Seq(contactInformationForOrg))

      result mustBe "Organisation name"
    }
  }

  "retrieveContactEmail" - {
    "must return the email from contact details if an individual" in {
      val result = viewHelper.retrieveContactEmail(Seq(contactInformationForInd))

      result mustBe "email@email.com"
    }

    "must return the email from contact details if an organisation" in {
      val result = viewHelper.retrieveContactEmail(Seq(contactInformationForOrg))

      result mustBe "email@email.com"
    }
  }

  "retrieveContactPhone" - {
    "must return the phone from contact details if an individual" in {
      val result = viewHelper.retrieveContactPhone(Seq(contactInformationForInd))

      result mustBe "07111222333"
    }

    "must return the phone from contact details if an organisation" in {
      val result = viewHelper.retrieveContactPhone(Seq(contactInformationForOrg))

      result mustBe "07111123333"
    }
  }

  "getPrimaryContactName" - {
    "must return the name stored in ContactNamePage if available" in {
      val userAnswers = UserAnswers(userAnswersId).set(ContactNamePage, "Contact name").success.value
      val result      = viewHelper.getPrimaryContactName(userAnswers)

      result mustBe "Contact name"
    }

    "must return the name from contact details stored in DisplaySubscriptionDetailsPage" in {
      val userAnswers = UserAnswers(userAnswersId).set(DisplaySubscriptionDetailsPage, displaySubscriptionDetails).success.value
      val result      = viewHelper.getPrimaryContactName(userAnswers)

      result mustBe "Organisation Name"
    }

    "must return the default if no name is available" in {
      val result = viewHelper.getPrimaryContactName(emptyUserAnswers)

      result mustBe "your first contact"
    }
  }

  "getSecondaryContactName" - {
    "must return the name stored in SecondaryContactNamePage if available" in {
      val userAnswers = UserAnswers(userAnswersId).set(SecondaryContactNamePage, "Contact name").success.value
      val result      = viewHelper.getSecondaryContactName(userAnswers)

      result mustBe "Contact name"
    }

    "must return the name from contact details stored in DisplaySubscriptionDetailsPage" in {
      val userAnswers = UserAnswers(userAnswersId).set(DisplaySubscriptionDetailsPage, displaySubscriptionDetails).success.value
      val result      = viewHelper.getSecondaryContactName(userAnswers)

      result mustBe "Secondary contact name"
    }

    "must return the default if no name is available" in {
      val result = viewHelper.getSecondaryContactName(emptyUserAnswers)

      result mustBe "your second contact"
    }
  }

  "Building contact details page" - {

    "must create row for Contact name" in {
      forAll(validDacID, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, email, secondaryEmail, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.primaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"Contact name"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.ContactNameController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactName.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-primary-contact-name")
                )
              )
            )

          val displayPayload: String = displaySubscriptionPayload(JsString(safeID),
                                                                  JsString("Contact name"),
                                                                  JsString("Secondary contact name"),
                                                                  JsString(email),
                                                                  JsString(secondaryEmail),
                                                                  JsString(phone)
          )
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse =
            Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.primaryContactName(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe Some(expectedRow)
      }
    }

    "must create row for Email address" in {
      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"$email"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.ContactEmailAddressController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-primary-contact-email")
                )
              )
            )

          val displayPayloadNoSecondaryContact: String =
            displaySubscriptionPayloadNoSecondary(JsString(safeID), JsString("Kit"), JsString("Kat"), JsString(email), JsString(phone))
          val displaySubscriptionDetailsNoSecondaryContact: DisplaySubscriptionForDACResponse =
            Json.parse(displayPayloadNoSecondaryContact).validate[DisplaySubscriptionForDACResponse].get

          val result =
            viewHelper.primaryContactEmail(displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for have telephone number question" in {
      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.haveContactPhone.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(msg"site.yes"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.HaveContactPhoneController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveContactPhone.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-have-contact-phone-number")
                )
              )
            )

          val displayPayloadNoSecondaryContact: String =
            displaySubscriptionPayloadNoSecondary(JsString(safeID), JsString("Kit"), JsString("Kat"), JsString(email), JsString(phone))
          val displaySubscriptionDetailsNoSecondaryContact: DisplaySubscriptionForDACResponse =
            Json.parse(displayPayloadNoSecondaryContact).validate[DisplaySubscriptionForDACResponse].get

          val result =
            viewHelper.haveContactPhoneNumber(displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for Telephone" in {
      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"$phone"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.ContactTelephoneNumberController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-primary-phone-number")
                )
              )
            )

          val displayPayloadNoSecondaryContact: String =
            displaySubscriptionPayloadNoSecondary(JsString(safeID), JsString("Kit"), JsString("Kat"), JsString(email), JsString(phone))
          val displaySubscriptionDetailsNoSecondaryContact: DisplaySubscriptionForDACResponse =
            Json.parse(displayPayloadNoSecondaryContact).validate[DisplaySubscriptionForDACResponse].get

          val result =
            viewHelper.primaryPhoneNumber(displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe Some(expectedRow)
      }
    }

    "must not create row for telephone if it doesn't exist" in {
      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, orgName, secondaryName, email, secondaryEmail, phone) =>
          val displayPayload: String =
            displaySubscriptionPayload(JsString(safeID), JsString(orgName), JsString(secondaryName), JsString(email), JsString(secondaryEmail), JsString(phone))
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.primaryPhoneNumber(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe None
      }
    }

    "must create row for have secondary contact question" in {
      forAll(validDacID, validEmailAddress, validPhoneNumber) {
        (safeID, email, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.haveSecondContact.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(msg"site.no"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.HaveSecondContactController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveSecondContact.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-have-second-contact")
                )
              )
            )

          val displayPayloadNoSecondaryContact: String =
            displaySubscriptionPayloadNoSecondary(JsString(safeID), JsString("Kit"), JsString("Kat"), JsString(email), JsString(phone))
          val displaySubscriptionDetailsNoSecondaryContact: DisplaySubscriptionForDACResponse =
            Json.parse(displayPayloadNoSecondaryContact).validate[DisplaySubscriptionForDACResponse].get

          val result =
            viewHelper.haveSecondaryContact(displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for Secondary contact name" in {
      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, orgName, secondaryName, email, secondaryEmail, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.secondaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"$secondaryName"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.SecondaryContactNameController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactName.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-secondary-contact-name")
                )
              )
            )

          val displayPayload: String =
            displaySubscriptionPayload(JsString(safeID), JsString(orgName), JsString(secondaryName), JsString(email), JsString(secondaryEmail), JsString(phone))
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.secondaryContactName(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for Secondary email address" in {
      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, orgName, secondaryContactName, email, secondaryEmail, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"$secondaryEmail"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.SecondaryContactEmailAddressController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-secondary-contact-email")
                )
              )
            )

          val displayPayload: String = displaySubscriptionPayload(JsString(safeID),
                                                                  JsString(orgName),
                                                                  JsString(secondaryContactName),
                                                                  JsString(email),
                                                                  JsString(secondaryEmail),
                                                                  JsString(phone)
          )
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.secondaryContactEmail(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for have secondary telephone number question" in {
      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, orgName, secondaryName, email, secondaryEmail, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.haveSecondContactPhone.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(msg"site.yes"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.HaveSecondaryContactPhoneController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveSecondContactPhone.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-have-second-contact-phone")
                )
              )
            )

          val displayPayload: String =
            displaySubscriptionPayload(JsString(safeID), JsString(orgName), JsString(secondaryName), JsString(email), JsString(secondaryEmail), JsString(phone))
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.haveSecondaryContactPhone(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe expectedRow
      }
    }

    "must create row for secondary telephone" in {
      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, orgName, secondaryName, email, secondaryEmail, phone) =>
          val expectedRow =
            Row(
              key = Key(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
              value = Value(lit"$phone"),
              actions = List(
                Action(
                  content = msg"site.edit",
                  href = controllers.contactdetails.routes.SecondaryContactTelephoneNumberController.onPageLoad().url,
                  visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel")),
                  attributes = Map("id" -> "change-secondary-phone-number")
                )
              )
            )

          val displayPayload: String =
            displaySubscriptionPayload(JsString(safeID), JsString(orgName), JsString(secondaryName), JsString(email), JsString(secondaryEmail), JsString(phone))
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.secondaryPhoneNumber(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe Some(expectedRow)
      }
    }

    "must not create row for Secondary telephone if it's not available" in {
      forAll(validDacID, validPersonalName, validEmailAddress, validPhoneNumber) {
        (safeID, name, email, phone) =>
          val displayPayload: String                                        = displaySubscriptionPayloadNoSecondary(JsString(safeID), JsString(name), JsString(name), JsString(email), JsString(phone))
          val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

          val result = viewHelper.secondaryPhoneNumber(displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

          result mustBe None
      }
    }
  }
}
