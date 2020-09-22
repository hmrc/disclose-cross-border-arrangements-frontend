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
import models.{GenericError, SubmissionDetails, SubmissionHistory}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, Table, _}

class ViewHelperSpec extends SpecBase with MockitoSugar {

  val viewHelper = new ViewHelper
  val mockURL = Json.toJson("www.test.com")

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
}
