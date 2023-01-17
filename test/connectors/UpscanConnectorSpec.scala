/*
 * Copyright 2023 HM Revenue & Customs
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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import models.upscan.{PreparedUpload, Reference, UploadForm, UpscanInitiateRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.WireMockHelper

class UpscanConnectorSpec extends SpecBase with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.upscan.port" -> server.port()
    )
    .build()

  lazy val connector: UpscanConnector = app.injector.instanceOf[UpscanConnector]
  val request: UpscanInitiateRequest  = UpscanInitiateRequest("callbackUrl", "successRedirectUrl", "errorRedirectUrl")

  "getUpscanFormData" - {
    "should return an UpscanInitiateResponse" - {
      "when upscan returns a valid successful response" in {
        val body = PreparedUpload(Reference("Reference"), UploadForm("downloadUrl", Map("formKey" -> "formValue")))
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(body).toString())
            )
        )

        whenReady(connector.getUpscanFormData) {
          result =>
            result mustBe body.toUpscanInitiateResponse
        }

      }
    }

    "throw an exception" - {
      "when upscan returns a 4xx response" in {
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )

        val result = connector.getUpscanFormData

        whenReady(result.failed) {
          e =>
            e mustBe an[UpstreamErrorResponse]
            val error = e.asInstanceOf[UpstreamErrorResponse]
            error.statusCode mustBe BAD_REQUEST
        }
      }

      "when upscan returns 5xx response" in {
        server.stubFor(
          post(urlEqualTo(connector.upscanInitiatePath))
            .willReturn(
              aResponse()
                .withStatus(SERVICE_UNAVAILABLE)
            )
        )

        val result = connector.getUpscanFormData
        whenReady(result.failed) {
          e =>
            e mustBe an[UpstreamErrorResponse]
            val error = e.asInstanceOf[UpstreamErrorResponse]
            error.statusCode mustBe SERVICE_UNAVAILABLE
        }
      }
    }
  }
}
