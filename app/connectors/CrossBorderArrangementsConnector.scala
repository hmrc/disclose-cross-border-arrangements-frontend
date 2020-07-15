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
import models.GeneratedIDs
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.SubmissionUtil._
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class CrossBorderArrangementsConnector @Inject()(configuration: FrontendAppConfig,
                                                 httpClient: HttpClient)(implicit val ec: ExecutionContext) {

  val submitUrl = s"${configuration.crossBorderArrangementsUrl}/disclose-cross-border-arrangements/submit"

  private val headers = Seq(
    HeaderNames.CONTENT_TYPE -> "application/xml"
  )

  def submitDocument(fileName: String, xmlDocument: Elem)(implicit hc: HeaderCarrier): Future[GeneratedIDs] =
    httpClient.POST[String, GeneratedIDs](submitUrl, constructSubmission(fileName, xmlDocument).toString(), headers)

}
