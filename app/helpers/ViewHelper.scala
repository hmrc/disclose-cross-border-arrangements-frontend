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
import models.{GenericError, SubmissionHistory}
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, Table}


class ViewHelper @Inject()() {

  def linkToHomePageText(href: JsValue)(implicit messages: Messages): Html = {
    Html(s"${{ messages("confirmation.link.text")}} <a id='homepage-link' href=$href>${{ messages("confirmation.link.text2") }}</a>.")
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
      attributes = Map("id" -> "disclosuresTable"))
  }
}
