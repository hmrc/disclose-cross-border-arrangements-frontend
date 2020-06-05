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
import javax.inject.{Inject, Named}
import models.upscan._
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class UpscanConnector @Inject()(configuration: FrontendAppConfig,
                                httpClient: HttpClient,
                                @Named("appName") appName: String)
                               (implicit ec: ExecutionContext){

  private val headers = Map(
    HeaderNames.USER_AGENT   -> appName,
    HeaderNames.CONTENT_TYPE -> "application/json"
  )

  def getUpscanFormData(body: UpscanInitiateRequest)
                       (implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {
    httpClient.POST[UpscanInitiateRequest, PreparedUpload](upscanInitiateUrl, body, headers.toSeq).map {
      _.toUpscanInitiateResponse
    }
  }

  private val upscanInitiateHost: String = configuration.upscanInitiateHost
  private[connectors] val upscanInitiatePath: String = "/upscan/v2/initiate"
  private val upscanInitiateUrl: String = upscanInitiateHost + upscanInitiatePath
}
