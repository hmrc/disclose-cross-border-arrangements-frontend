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
import models.{GenericError, UserAnswers}
import pages.ValidXMLPage
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels._

class CheckYourAnswersHelper(userAnswers: UserAnswers)(implicit messages: Messages) {

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

  def mapErrorsToTable(listOfErrors: Seq[GenericError]) : Table = {

    val head: Seq[Cell] = Seq(
      Cell(msg"invalidXML.table.heading1", classes = Seq("govuk-!-width-one-quarter", "govuk-table__header")),
      Cell(msg"invalidXML.table.heading2", classes = Seq("govuk-!-font-weight-bold"))
    )

    val rows: Seq[Seq[Cell]] =
      for {
        errors <- listOfErrors.sorted
      } yield {
        Seq(
          Cell(msg"${errors.lineNumber}", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> "lineNumber")),
          Cell(msg"${errors.messageKey}", classes = Seq("govuk-table__cell"), attributes = Map("id" -> "errorMessage"))
        )
      }

    Table(
      head = head,
      rows = rows,
      caption = Some(msg"invalidXML.h3"),
      attributes = Map("id" -> "errorTable", "aria-describedby" -> messages("invalidXML.h3")))
  }

}

object CheckYourAnswersHelper {
  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
}


