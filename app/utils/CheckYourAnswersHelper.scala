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

package utils

import java.time.format.DateTimeFormatter

import controllers.routes
import models.UserAnswers
import pages.ValidXMLPage
import play.api.i18n.Messages
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels._

class CheckYourAnswersHelper(userAnswers: UserAnswers)(implicit messages: Messages) {

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

  def displaySummaryFromInstruction(importInstruction: String, arrangementID: String, disclosureID: String, messageRefID: String): Seq[SummaryList.Row] =
    importInstruction match {
      case "DAC6NEW" =>
        Seq(
          uploadedFile.get,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.new.text", classes = Seq("new-arrangement-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      case "DAC6ADD" =>
        Seq(
          uploadedFile.get,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.additional.text".withArgs(arrangementID), classes = Seq("additional-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      case "DAC6REP" =>
        Seq(
          uploadedFile.get,
          Row(
            key = Key(msg"checkYourAnswers.disclosure.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.replacement.text".withArgs(disclosureID, arrangementID), classes = Seq("replacement-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
      case _ =>
        Seq(
          uploadedFile.get,
          Row(
            key = Key(msg"checkYourAnswers.deleteFile", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(msg"checkYourAnswers.deleteDisclosure.text".withArgs(disclosureID, arrangementID), classes = Seq("delete-disclosure-text"))
          ),
          Row(
            key = Key(msg"checkYourAnswers.messageRefID.text", classes = Seq("govuk-!-width-one-third disclosing-key")),
            value = Value(lit"$messageRefID", classes = Seq("messageRefID"))
          )
        )
    }
}

object CheckYourAnswersHelper {
  DateTimeFormatter.ofPattern("d MMMM yyyy")
}
