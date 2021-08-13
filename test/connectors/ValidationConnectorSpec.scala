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
import controllers.exceptions.UpscanTimeoutException
import generators.Generators
import helpers.JsonFixtures
import models.{Dac6MetaData, GenericError, ValidationErrors}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsString
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidationConnectorSpec extends SpecBase with WireMockHelper with Generators with ScalaCheckPropertyChecks with ScalaFutures {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.cross-border-arrangements.port" -> server.port()
    )
    .build()

  lazy val connector: ValidationConnector = app.injector.instanceOf[ValidationConnector]
  val validationUrl                       = "/disclose-cross-border-arrangements/validate-upload-submission"

  val successPayloadResult: Dac6MetaData = Dac6MetaData(
    importInstruction = "DAC6NEW",
    arrangementID = Some("A123"),
    disclosureID = Some("D123"),
    disclosureInformationPresent = true,
    initialDisclosureMA = true,
    messageRefId = "messageRef"
  )

  val failurePayloadResult: ValidationErrors = ValidationErrors(Seq(GenericError(1, "some error"), GenericError(2, "another error")), None)

  "Validation Connector" - {

    "must return a 200 and a Success Object when passing validation" in {

      val expectedBody = JsonFixtures.xmlValidationSuccessResponse(JsString("DAC6NEW"), JsString("A123"), JsString("D123"), JsString("messageRef"))

      stubResponse(validationUrl, OK, expectedBody)

      val result = connector.sendForValidation("SomeUrl")
      result.futureValue mustBe Some(Right(successPayloadResult))
    }

    "must return a 200 and a Failure Object when failing validation" in {

      val expectedBody = JsonFixtures.xmlValidationFailureResponse

      stubResponse(validationUrl, OK, expectedBody)

      val result = connector.sendForValidation("SomeUrl")
      result.futureValue mustBe Some(Left(failurePayloadResult))
    }

    "must throw an exception when validation returns a 400 (BAD_REQUEST) status" in {
      stubResponse(validationUrl, BAD_REQUEST, "Some error")

      val result = connector.sendForValidation("SomeUrl")

      whenReady(result.failed)(_ mustBe a[UpscanTimeoutException])
    }
  }

  private def stubResponse(expectedUrl: String, expectedStatus: Int, expectedBody: String): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedBody)
        )
    )
}
