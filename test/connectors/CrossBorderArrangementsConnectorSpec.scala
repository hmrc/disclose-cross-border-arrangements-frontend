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

import java.time.LocalDateTime

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlEqualTo}
import models.{GeneratedIDs, SubmissionDetails, SubmissionHistory}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

class CrossBorderArrangementsConnectorSpec extends SpecBase
  with GuiceOneAppPerSuite
  with MockitoSugar
  with WireMockHelper
  with ScalaFutures{

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.cross-border-arrangements.port" -> server.port()
    ).build()

  lazy val connector: CrossBorderArrangementsConnector = app.injector.instanceOf[CrossBorderArrangementsConnector]

  "Cross Border Arrangements Connector" - {
    "should return a GeneratedIDs" - {
      "when the backend returns a valid successful response" in {
        val json = Json.obj(
          "arrangementID" -> "GBA20200601AAA000",
          "disclosureID" -> "GBD20200601AAA001"
        )


        server.stubFor(
          post(urlEqualTo("/disclose-cross-border-arrangements/submit"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(json.toString())
            )
        )

        val xml = <test></test>

        whenReady(connector.submitDocument("test-file.xml", "enrolmentID", xml)){
          result =>
            result mustBe GeneratedIDs(Some("GBA20200601AAA000"), Some("GBD20200601AAA001"))
        }
      }
    }

    "throw an exception" - {
      "when upscan returns a 4xx response" in {
        server.stubFor(
          post(urlEqualTo("/disclose-cross-border-arrangements/submit"))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )

        val xml = <test></test>
        val result = connector.submitDocument("test-file.xml","enrolmentID", xml)

        whenReady(result.failed){ e =>
          e mustBe a[UpstreamErrorResponse]
          val error = e.asInstanceOf[UpstreamErrorResponse]
          error.statusCode mustBe BAD_REQUEST
        }
      }

      "when upscan returns 5xx response" in {
        server.stubFor(
          post(urlEqualTo("/disclose-cross-border-arrangements/submit"))
            .willReturn(
              aResponse()
                .withStatus(SERVICE_UNAVAILABLE)
            )
        )

        val xml = <test></test>
        val result = connector.submitDocument("test-file.xml", "enrolmentID", xml)
        whenReady(result.failed){ e =>
          e mustBe an[UpstreamErrorResponse]
          val error = e.asInstanceOf[UpstreamErrorResponse]
          error.statusCode mustBe SERVICE_UNAVAILABLE
        }
      }
    }

    "should return a submission details returned from backend" in {
      val json = Json.obj(
        "details" -> JsArray(Seq(Json.obj(
          "enrolmentID" -> "enrolmentID",
          "submissionTime" -> "2007-12-03T10:15:30",
          "fileName" -> "fileName",
          "importInstruction" -> "New",
          "initialDisclosureMA" -> false
        )))
      )

      server.stubFor(
        get(urlEqualTo("/disclose-cross-border-arrangements/history/submissions/enrolmentID"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(json.toString())
          )
      )

      whenReady(connector.retrievePreviousSubmissions("enrolmentID")) {
        result =>
          result mustBe SubmissionHistory(
            Seq(
              SubmissionDetails(
                "enrolmentID",
                LocalDateTime.parse("2007-12-03T10:15:30"),
                "fileName",
                None,
                None,
                "New",
                initialDisclosureMA = false
              )
            )
          )
      }
    }

  }
}
