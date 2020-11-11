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

package utils

import java.time.format.DateTimeFormatter

import controllers.routes
import models.UserAnswers
import pages.ValidXMLPage
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels._

class CheckYourAnswersHelper(userAnswers: UserAnswers)(implicit messages: Messages) {

//  def individualContactName: Option[Row] = userAnswers.get(IndividualContactNamePage) map {
//    answer =>
//      Row(
//        key     = Key(msg"individualContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"${answer.firstName} ${answer.lastName}"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.IndividualContactNameController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"individualContactName.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def secondaryContactTelephoneNumber: Option[Row] = userAnswers.get(SecondaryContactTelephoneNumberPage) map {
//    answer =>
//      Row(
//        key     = Key(msg"secondaryContactTelephoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.SecondaryContactTelephoneNumberController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"secondaryContactTelephoneNumber.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def secondaryContactEmailAddress: Option[Row] = userAnswers.get(SecondaryContactEmailAddressPage) map {
//    answer =>
//      Row(
//        key     = Key(msg"secondaryContactEmailAddress.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.SecondaryContactEmailAddressController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"secondaryContactEmailAddress.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def secondaryContactName: Option[Row] = userAnswers.get(SecondaryContactNamePage) map {
//    answer =>
//      Row(
//        key     = Key(msg"secondaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.SecondaryContactNameController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"secondaryContactName.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def contactTelephoneNumber: Option[Row] = userAnswers.get(ContactTelephoneNumberPage) map {
//    answer =>
//      Row(
//        key     = Key(msg"contactTelephoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.ContactTelephoneNumberController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactTelephoneNumber.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def contactEmailAddress: Option[Row] = userAnswers.get(ContactEmailAddressPage) map {
//    answer =>
//      Row(
//        key     = Key(msg"contactEmailAddress.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.ContactEmailAddressController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactEmailAddress.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

//  def contactName: Option[Row] = userAnswers.get(ContactNamePage) map {
//    answer =>
//      Row(
//        key     = Key(msg"contactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-half")),
//        value   = Value(lit"$answer"),
//        actions = List(
//          Action(
//            content            = msg"site.edit",
//            href               = routes.ContactNameController.onPageLoad(CheckMode).url,
//            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactName.checkYourAnswersLabel"))
//          )
//        )
//      )
//  }

  private def yesOrNo(answer: Boolean)(implicit messages: Messages): Html = {
    if (answer) {
      HtmlFormat.escape(messages("site.yes"))
    } else {
      HtmlFormat.escape(messages("site.no"))
    }
  }

  def uploadedFile: Option[Row] = userAnswers.get(ValidXMLPage) map {
    fileName =>
      Row(
        key = Key(msg"checkYourAnswers.uploadedFile", classes = Seq("govuk-!-width-one-third")),
        value = Value(lit"$fileName"),
        actions = List(
          Action(
            content = msg"site.edit",
            href = routes.UploadFormController.onPageLoad().url,
            visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"checkYourAnswers.uploadedFile")),
            attributes = Map("id" -> "change-link")
          )
        )
      )
  }

  def displaySummaryFromInstruction(importInstruction: String, arrangementID: String, disclosureID: String) : Seq[SummaryList.Row] = {

    importInstruction match {
      case "DAC6NEW" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
        value = Value(msg"checkYourAnswers.new.text", classes = Seq("new-arrangement-text"))
        )
      )
      case "DAC6ADD" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
        value = Value(msg"checkYourAnswers.additional.text".withArgs(arrangementID), classes = Seq("additional-disclosure-text"))
        )
      )
      case "DAC6REP" => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
        value = Value(msg"checkYourAnswers.replacement.text".withArgs(disclosureID, arrangementID), classes = Seq("replacement-disclosure-text"))
        )
      )
      case _ => Seq(uploadedFile.get, Row(
        key = Key(msg"checkYourAnswers.deleteFile", classes = Seq("govuk-!-width-one-third disclosing-key")),
        value = Value(msg"checkYourAnswers.deleteDisclosure.text".withArgs(disclosureID, arrangementID), classes = Seq("delete-disclosure-text"))
        )
      )
    }
  }
}

object CheckYourAnswersHelper {
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
}


