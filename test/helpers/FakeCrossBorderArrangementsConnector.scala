/*
 * Copyright 2021 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.CrossBorderArrangementsConnector
import models.upscan.{Reference, UploadId, UploadSessionDetails, UploadStatus}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeCrossBorderArrangementsConnector @Inject()(configuration: FrontendAppConfig,
                                                     httpClient: HttpClient)(implicit ec: ExecutionContext)
  extends CrossBorderArrangementsConnector(configuration, httpClient){

  //TODO: I know!
  var statusBuffer: Option[UploadStatus] = None
  var detailsBuffer: Option[UploadSessionDetails] = None

  def setStatus(uploadStatus: UploadStatus): Unit = {
    statusBuffer = Some(uploadStatus)
  }

  def setDetails(uploadDetails: UploadSessionDetails): Unit = {
    detailsBuffer = Some(uploadDetails)
  }

  override def requestUpload(uploadId: UploadId, fileReference: Reference)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    Future.successful(HttpResponse(200, ""))

  override def getUploadStatus(uploadId: UploadId)(implicit hc: HeaderCarrier): Future[Option[UploadStatus]] = {
    Future.successful(statusBuffer)
  }

  override def getUploadDetails(uploadId: UploadId)(implicit hc: HeaderCarrier): Future[Option[UploadSessionDetails]] = {
    Future.successful(detailsBuffer)
  }

}
