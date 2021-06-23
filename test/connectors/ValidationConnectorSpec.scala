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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import generators.Generators
import helpers.JsonFixtures
import models.Dac6MetaData
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsString
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class ValidationConnectorSpec extends SpecBase
  with WireMockHelper
  with Generators
  with ScalaCheckPropertyChecks {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.cross-border-arrangements.port" -> server.port()
    ).build()

  lazy val connector: ValidationConnector = app.injector.instanceOf[ValidationConnector]
  val validationUrl = "/disclose-cross-border-arrangements/validate-upload-submission"

  val successPayload = Dac6MetaData(
    importInstruction = "DAC6NEW",
    arrangementID = Some("A123"),
    disclosureID = Some("D123"),
    disclosureInformationPresent = true,
    initialDisclosureMA = true,
    messageRefId = "messageRef"
  )

  val failurePayload =
    """
      |{
      | "errors": ["message1", "message2"]
      |}""".stripMargin

  "Validation Connector" - {
    "must return a 200 and a Success Object when passing validation" in {

      val expectedBody = JsonFixtures.xmlValidationResponseJson(JsString("DAC6NEW"), JsString("A123"), JsString("D123"), JsString("messageRef"))

      stubResponse(validationUrl, OK, expectedBody)

      val result = connector.sendForValidation(<test></test>)
      result.futureValue mustBe Right(successPayload)
    }

//    "must return a 200 and a Failure Object when failing validation" in {
//      stubResponse(validationUrl, OK, failurePayload)
//
//      val result = connector.sendForValidation(<test></test>)
//      result.futureValue mustBe Left(Seq("message1", "message2"))
//    }

//    "must throw an exception when validation returns a 400 (BAD_REQUEST) status" in {
//      stubResponse(validationUrl, BAD_REQUEST, "Some error")
//
//      val result = connector.sendForValidation(<test></test>)
//
//      assertThrows[Exception] {
//        result.futureValue
//      }
    }

//    "must throw an exception when address lookup returns a 404 (NOT_FOUND) status" in {
//      stubResponse(validationUrl, NOT_FOUND, "Some error")
//
//      val result = connector.sendForValidation(<test></test>)
//
//      assertThrows[Exception] {
//        result.futureValue
//      }
//    }
//
//    "must throw an exception when validation returns a 405 (METHOD_NOT_ALLOWED) status" in {
//      stubResponse(validationUrl, METHOD_NOT_ALLOWED, "Some error")
//
//      val result = connector.sendForValidation(<test></test>)
//
//      assertThrows[Exception] {
//        result.futureValue
//      }
//    }
//
//    "must throw an exception when validation returns a 500 (INTERNAL_SERVER_ERROR) status" in {
//      stubResponse(validationUrl, INTERNAL_SERVER_ERROR, "Some error")
//
//      val result = connector.sendForValidation(<test></test>)
//
//      assertThrows[Exception] {
//        result.futureValue
//      }
//    }
//  }

  private def stubResponse(expectedUrl: String, expectedStatus: Int, expectedBody: String = ""): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedBody)
        )
    )
}
