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

package connectors

import config.FrontendAppConfig
import javax.inject.Inject
import models.{GeneratedIDs, SubmissionDetails, SubmissionHistory}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.SubmissionUtil._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class CrossBorderArrangementsConnector @Inject()(configuration: FrontendAppConfig,
                                                 httpClient: HttpClient)(implicit val ec: ExecutionContext) {

  val baseUrl = s"${configuration.crossBorderArrangementsUrl}/disclose-cross-border-arrangements"
  val submitUrl = s"$baseUrl/submit"

  private val headers = Seq(
    HeaderNames.CONTENT_TYPE -> "application/xml"
  )

  def submitDocument(fileName: String, enrolmentID: String, xmlDocument: Elem)(implicit hc: HeaderCarrier): Future[GeneratedIDs] = {
    httpClient.POSTString[GeneratedIDs](submitUrl, constructSubmission(fileName, enrolmentID, xmlDocument).toString(), headers)
  }

  def findNoOfPreviousSubmissions(enrolmentID: String)(implicit hc: HeaderCarrier): Future[Long] =
    httpClient.GET[Long](s"$baseUrl/history/count/$enrolmentID")

  //TODO: should have paging to support large no of filings
  def retrievePreviousSubmissions(enrolmentID: String)(implicit hc: HeaderCarrier): Future[SubmissionHistory] =
    httpClient.GET[SubmissionHistory](s"$baseUrl/history/submissions/$enrolmentID")

  def retrieveFirstDisclosureForArrangementID(arrangementID: String)(implicit hc: HeaderCarrier): Future[SubmissionDetails] =
    httpClient.GET[SubmissionDetails](s"$baseUrl/history/first-disclosure/$arrangementID")

}
