/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDateTime

import base.SpecBase
import controllers.routes
import helpers.JsonFixtures.{displaySubscriptionPayload, displaySubscriptionPayloadNoSecondary}
import models.subscription._
import models.{GenericError, SubmissionDetails, SubmissionHistory}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, Table, _}

class ViewHelperSpec extends SpecBase with MockitoSugar {

  val viewHelper = new ViewHelper
  val mockURL: JsValue = Json.toJson("www.test.com")

  val displayPayload: String = displaySubscriptionPayload(
    JsString("Kinder"), JsString("Bueno"), JsString("Kinder Bueno"), JsString("email@email.com"),
    JsString("email2@email.com"), JsString("07111222333"))
  val displaySubscriptionDetails: DisplaySubscriptionForDACResponse = Json.parse(displayPayload).validate[DisplaySubscriptionForDACResponse].get

  val displayPayloadNoSecondaryContact: String = displaySubscriptionPayloadNoSecondary(
    JsString("Kit"), JsString("Kat"), JsString("email@email.org"), JsString("07111112222"))
  val displaySubscriptionDetailsNoSecondaryContact: DisplaySubscriptionForDACResponse =
    Json.parse(displayPayloadNoSecondaryContact).validate[DisplaySubscriptionForDACResponse].get

  "linkToHomePageText" - {

    "must return the correct go to home page content" in {

      viewHelper.linkToHomePageText(mockURL) mustBe Html(s"Go to <a id='homepage-link' href=$mockURL>" +
        s"disclose cross-border arrangements</a>.")

    }
  }

  "surveyLinkText" - {

    "must return the correct beta feedback content" in {

      viewHelper.surveyLinkText(mockURL) mustBe Html(s"<a id='feedback-link' href=$mockURL>" +
        s"What did you think of this service?</a> (takes 30 seconds)")

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
        rows = Seq(Seq(
          Cell(msg"11", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_11")),
          Cell(msg"Enter your cats name in meow format", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_11"))
        )),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }

    "must return a table containing multiple rows with line numbers & errors when a given Generic Error" in {

      val mockMultiError: Seq[GenericError] =
        Seq(GenericError(22, "Enter cat years only"),
          GenericError(33,"Incorrect number of cat legs"))

      viewHelper.mapErrorsToTable(mockMultiError) mustBe Table(
        head = head,
        rows = Seq(Seq(
          Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_22")),
          Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_22"))
        ),
          Seq(
            Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_33")),
            Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_33"))
          )),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }

    "must return a table containing multiple rows in correct order with line numbers & errors when a given Generic Error" in {

      val mockMultiError: Seq[GenericError] =
        Seq(GenericError(33,"Incorrect number of cat legs"),
          GenericError(48, "You gotta be kitten me"),
          GenericError(22,"Enter cat years only"))

      viewHelper.mapErrorsToTable(mockMultiError) mustBe Table(
        head = head,
        rows = Seq(Seq(
          Cell(msg"22", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_22")),
          Cell(msg"Enter cat years only", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_22"))),
          Seq(
            Cell(msg"33", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_33")),
            Cell(msg"Incorrect number of cat legs", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_33"))),
          Seq(
            Cell(msg"48", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber_48")),
            Cell(msg"You gotta be kitten me", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage_48"))
          )),
        caption = Some(msg"invalidXML.h3"),
        attributes = Map("id" -> "errorTable")
      )
    }
  }

  "buildDisclosuresTable" - {
    "must return a table of the submission history" in {
      val head: Seq[Cell] = Seq(
        Cell(msg"submissionHistory.arn.label", classes = Seq("govuk-!-width-one-quarter")),
        Cell(msg"submissionHistory.disclosureID.label", classes = Seq("govuk-!-width-one-quarter")),
        Cell(msg"submissionHistory.submissionDate.label", classes = Seq("govuk-table__header")),
        Cell(msg"submissionHistory.fileName.label", classes = Seq("govuk-!-width-one-quarter"))
      )

      val mockSubmissionHistory = SubmissionHistory(
        List(
          SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-07-01T10:23:30"),
            "fileName", Some("arrangementID"), Some("disclosureID"), "New", initialDisclosureMA = false),
          SubmissionDetails("enrolmentI2", LocalDateTime.parse("2020-07-02T20:23:30"),
            "fileName2", Some("arrangementID2"), Some("disclosureID2"), "Add", initialDisclosureMA = false),
        )
      )

      viewHelper.buildDisclosuresTable(mockSubmissionHistory) mustBe Table(
        head = head,
        rows = Seq(
          Seq(
            Cell(msg"arrangementID", attributes = Map("id" -> s"arrangementID_0")),
            Cell(msg"disclosureID", attributes = Map("id" -> s"disclosureID_0")),
            Cell(msg"10:23am on 1 July 2020", attributes = Map("id" -> s"submissionTime_0")),
            Cell(msg"fileName", attributes = Map("id" -> s"fileName_0"))
          ),
          Seq(
            Cell(msg"arrangementID2", attributes = Map("id" -> s"arrangementID_1")),
            Cell(msg"disclosureID2", attributes = Map("id" -> s"disclosureID_1")),
            Cell(msg"08:23pm on 2 July 2020", attributes = Map("id" -> s"submissionTime_1")),
            Cell(msg"fileName2", attributes = Map("id" -> s"fileName_1"))
          )
        ),
        caption = Some(msg"submissionHistory.caption"),
        attributes = Map("id" -> "disclosuresTable")
      )
    }
  }

  "buildDisplaySubscription" - {
    "must return the rows containing the subscription details" in {

      val primaryContact: PrimaryContact = PrimaryContact(Seq(
        ContactInformationForIndividual(
          individual = IndividualDetails(firstName = "John", lastName = "Business", middleName = None),
          email = "email@email.com", phone = Some("07111222333"), mobile = Some("07111222333"))
      ))

      val secondaryContact: SecondaryContact = SecondaryContact(Seq(
        ContactInformationForOrganisation(
          organisation = OrganisationDetails(organisationName = "Organisation Name"),
          email = "email2@email.com", phone = None, mobile = None)
      ))

      val responseDetail: ResponseDetail = ResponseDetail(
        subscriptionID = "XE0001234567890",
        tradingName = Some("Trading Name"),
        isGBUser = true,
        primaryContact = primaryContact,
        secondaryContact = Some(secondaryContact))

      val expectedRows = Seq(
        Seq(
          Cell(msg"displaySubscriptionForDAC.subscriptionID", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"XE0001234567890", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "subscriptionID"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.tradingName", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"Trading Name", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "tradingName"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.isGBUser", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"true", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "isGBUser"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualContact", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"John Business", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "individualContact"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualEmail", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"email@email.com", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "individualEmail"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualPhone", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"07111222333", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "individualPhone"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.individualMobile", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"07111222333", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "individualMobile"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationContact", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"Organisation Name", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "organisationContact"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationEmail", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"email2@email.com", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "organisationEmail"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationPhone", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"None", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "organisationPhone"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.organisationMobile", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"None", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "organisationMobile"))
        )
      )

      val result = viewHelper.buildDisplaySubscription(responseDetail, hasSecondContact = true)

      result mustBe Table(
        head = Seq(
          Cell(msg"Information", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"Value", classes = Seq("govuk-!-width-one-third"))
        ),
        rows = expectedRows)
    }
  }

  "Building contact details page" - {

    "must create row for Contact name" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.primaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"Kit Kat"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.IndividualContactNameController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactName.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-primary-contact-name")
            )
          )
        )

      val result = viewHelper.primaryContactName(
        displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }

    "must create row for Email address" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"email@email.org"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.ContactEmailAddressController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-primary-contact-email")
            )
          )
        )

      val result = viewHelper.primaryContactEmail(
        displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }

    "must create row for Telephone" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"07111112222"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.ContactTelephoneNumberController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-primary-phone-number")
            )
          )
        )

      val result = viewHelper.primaryPhoneNumber(
        displaySubscriptionDetailsNoSecondaryContact.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }

    "must create row for Secondary contact name" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.secondaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"Kinder Bueno"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.SecondaryContactNameController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactName.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-secondary-contact-name")
            )
          )
        )

      val result = viewHelper.secondaryContactName(
        displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }

    "must create row for Secondary email address" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"email2@email.com"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.SecondaryContactEmailAddressController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-secondary-contact-email")
            )
          )
        )

      val result = viewHelper.secondaryContactEmail(
        displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }

    "must create row for Secondary telephone - None if it's missing" in {
      val expectedRow =
        Row(
          key = Key(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"None"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = routes.SecondaryContactTelephoneNumberController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-secondary-phone-number")
            )
          )
        )

      val result = viewHelper.secondaryPhoneNumber(
        displaySubscriptionDetails.displaySubscriptionForDACResponse.responseDetail, emptyUserAnswers)

      result mustBe expectedRow
    }
  }
}
