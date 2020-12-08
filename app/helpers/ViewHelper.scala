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

import java.time.format.DateTimeFormatter

import com.google.inject.Inject
import controllers.routes
import models.subscription.{ContactInformation, ContactInformationForIndividual, ContactInformationForOrganisation, ResponseDetail}
import models.{GenericError, SubmissionHistory, UserAnswers}
import pages._
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, Table}


class ViewHelper @Inject()() {

  def linkToHomePageText(href: JsValue)(implicit messages: Messages): Html = {
    Html(s"<a id='homepage-link' href=$href>${{ messages("confirmation.link.text") }}</a>.")
  }

  def surveyLinkText(href: JsValue)(implicit messages: Messages): Html = {
    Html(s"<a id='feedback-link' href=$href>${{ messages("confirmation.survey.link")}}</a> ${{ messages("confirmation.survey.text")}}")
  }

  def mapErrorsToTable(listOfErrors: Seq[GenericError])(implicit messages: Messages) : Table = {

    val rows: Seq[Seq[Cell]] =
      for {
        error <- listOfErrors.sorted
      } yield {
        Seq(
          Cell(msg"${error.lineNumber}", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> s"lineNumber_${error.lineNumber}")),
          Cell(msg"${error.messageKey}", classes = Seq("govuk-table__cell"), attributes = Map("id" -> s"errorMessage_${error.lineNumber}"))
        )
      }

    Table(
      head = Seq(
        Cell(msg"invalidXML.table.heading1", classes = Seq("govuk-!-width-one-quarter", "govuk-table__header")),
        Cell(msg"invalidXML.table.heading2", classes = Seq("govuk-!-font-weight-bold"))),
      rows = rows,
      caption = Some(msg"invalidXML.h3"),
      attributes = Map("id" -> "errorTable"))
  }

  def buildDisclosuresTable(retrievedHistory: SubmissionHistory)(implicit messages: Messages) : Table = {

    val submissionDateFormat = DateTimeFormatter.ofPattern("hh:mma 'on' d MMMM yyyy")

    val rows = retrievedHistory.details.zipWithIndex.map {
      case (submission, count) =>
        Seq(
          Cell(msg"${submission.arrangementID.get}", attributes = Map("id" -> s"arrangementID_$count")),
          Cell(msg"${submission.disclosureID.get}", attributes = Map("id" -> s"disclosureID_$count")),
          Cell(msg"${submission.submissionTime.format(submissionDateFormat)
                    .replace("AM", "am")
                    .replace("PM","pm")}", attributes = Map("id" -> s"submissionTime_$count")),
          Cell(msg"${submission.fileName}", attributes = Map("id" -> s"fileName_$count"))
        )
    }

    Table(
      head = Seq(
        Cell(msg"submissionHistory.arn.label", classes = Seq("govuk-!-width-one-quarter")),
        Cell(msg"submissionHistory.disclosureID.label", classes = Seq("govuk-!-width-one-quarter")),
        Cell(msg"submissionHistory.submissionDate.label", classes = Seq("govuk-table__header")),
        Cell(msg"submissionHistory.fileName.label", classes = Seq("govuk-!-width-one-quarter"))
      ),
      rows = rows,
      caption = Some(msg"submissionHistory.caption"),
      attributes = Map("id" -> "disclosuresTable"))
  }

  def buildDisplaySubscription(responseDetail: ResponseDetail, hasSecondContact: Boolean): Table = {
    val rows =
      Seq(
        Seq(
          Cell(msg"displaySubscriptionForDAC.subscriptionID", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.subscriptionID}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "subscriptionID"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.tradingName", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.tradingName.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "tradingName"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.isGBUser", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.isGBUser}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "isGBUser"))
        )
      ) ++ buildContactDetails(responseDetail.primaryContact.contactInformation)

    val updateRows = if (hasSecondContact) {
      rows ++ buildContactDetails(
        responseDetail.secondaryContact.fold(Seq[ContactInformation]())(p => p.contactInformation)
      )
    } else {
      rows
    }

    Table(
      head = Seq(
        Cell(msg"Information", classes = Seq("govuk-!-width-one-third")),
        Cell(msg"Value", classes = Seq("govuk-!-width-one-third"))
      ),
      rows = updateRows)
  }

  private def buildContactDetails(contactInformation: Seq[ContactInformation]): Seq[Seq[Cell]] = {
    contactInformation.head match {
      case ContactInformationForIndividual(individual, email, phone, mobile) =>
        Seq(
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualContact", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${individual.firstName} ${individual.middleName.fold("")(mn => s"$mn ")}${individual.lastName}",
              classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualContact"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualEmail", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"$email", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualEmail"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualPhone", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${phone.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualPhone"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualMobile", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${mobile.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualMobile"))
          )
        )
      case ContactInformationForOrganisation(organisation, email, phone, mobile) =>
        Seq(
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationContact", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${organisation.organisationName}",
              classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationContact"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationEmail", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"$email", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationEmail"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationPhone", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${phone.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationPhone"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationMobile", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${mobile.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationMobile"))
          )
        )
    }
  }


  def primaryContactName(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactName = (userAnswers.get(ContactNamePage), userAnswers.get(IndividualContactNamePage)) match {
      case (Some(contactName), _) => s"${contactName.firstName} ${contactName.lastName}"
      case (_, Some(contactName)) => s"${contactName.firstName} ${contactName.lastName}"
      case _ =>
        responseDetail.primaryContact.contactInformation.head match {
          case ContactInformationForIndividual(individual, _, _, _) =>
            s"${individual.firstName} ${individual.middleName.fold("")(mn => s"$mn ")}${individual.lastName}"
          case ContactInformationForOrganisation(organisation, _, _, _) =>
            s"${organisation.organisationName}"
        }
    }

    val changeLink = responseDetail.primaryContact.contactInformation.head match {
      case ContactInformationForIndividual(_, _, _, _) =>
        routes.IndividualContactNameController.onPageLoad().url
      case ContactInformationForOrganisation(_, _, _, _) =>
        routes.ContactNameController.onPageLoad().url
    }

    Row(
      key = Key(msg"contactDetails.primaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactName"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = changeLink,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactName.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-primary-contact-name")
        )
      )
    )
  }

  def primaryContactEmail(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactEmail = userAnswers.get(ContactEmailAddressPage) match {
      case Some(email) => email
      case None =>
        responseDetail.primaryContact.contactInformation.head match {
          case ContactInformationForIndividual(_, email, _, _) => email
          case ContactInformationForOrganisation(_, email, _, _) => email
        }
    }

    Row(
      key = Key(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactEmail"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.ContactEmailAddressController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-primary-contact-email")
        )
      )
    )
  }

  def primaryPhoneNumber(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val phoneNumber = userAnswers.get(ContactTelephoneNumberPage) match {
      case Some(telephone) => telephone
      case None =>
        responseDetail.primaryContact.contactInformation.head match {
          case ContactInformationForIndividual(_, _, phone, _) => s"${phone.getOrElse("None")}"
          case ContactInformationForOrganisation(_, _, phone, _) => s"${phone.getOrElse("None")}"
        }
    }

    Row(
      key = Key(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$phoneNumber"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.ContactTelephoneNumberController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-primary-phone-number")
        )
      )
    )
  }

  def secondaryContactName(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    //Note: Secondary contact name is only one field in the registration journey. It's always ContactInformationForOrganisation
    val contactName = userAnswers.get(SecondaryContactNamePage) match {
      case Some(contactName) => contactName
      case None =>
        contactInformationList.head match {
          case ContactInformationForIndividual(individual, _, _, _) =>
            s"${individual.firstName} ${individual.middleName.fold("")(mn => s"$mn ")}${individual.lastName}"
          case ContactInformationForOrganisation(organisation, _, _, _) =>
            s"${organisation.organisationName}"
        }
    }

    Row(
      key = Key(msg"contactDetails.secondaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactName"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.SecondaryContactNameController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactName.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-secondary-contact-name")
        )
      )
    )
  }

  def secondaryContactEmail(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    val contactEmail = userAnswers.get(SecondaryContactEmailAddressPage) match {
      case Some(email) => email
      case None =>
        contactInformationList.head match {
          case ContactInformationForIndividual(_, email, _, _) => email
          case ContactInformationForOrganisation(_, email, _, _) => email
        }
    }

    Row(
      key = Key(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactEmail"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.SecondaryContactEmailAddressController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-secondary-contact-email")
        )
      )
    )
  }

  def secondaryPhoneNumber(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    val phoneNumber = userAnswers.get(SecondaryContactTelephoneNumberPage) match {
      case Some(telephone) => telephone
      case None =>
        contactInformationList.head match {
          case ContactInformationForIndividual(_, _, phone, _) => s"${phone.getOrElse("None")}"
          case ContactInformationForOrganisation(_, _, phone, _) => s"${phone.getOrElse("None")}"
        }
    }

    Row(
      key = Key(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$phoneNumber"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = routes.SecondaryContactTelephoneNumberController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-secondary-phone-number")
        )
      )
    )
  }
}
