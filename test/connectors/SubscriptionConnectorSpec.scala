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
import helpers.JsonFixtures._
import models.UserAnswers
import models.subscription.{UpdateSubscriptionDetails, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.contactdetails.ContactNamePage
import play.api.Application
import play.api.http.Status.{OK, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsString
import uk.gov.hmrc.http.HttpClient
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionConnectorSpec extends SpecBase with ScalaCheckPropertyChecks with Generators with BeforeAndAfterEach with WireMockHelper {

  val individualPrimaryContact: PrimaryContact = PrimaryContact(
    Seq(
      ContactInformationForIndividual(individual = IndividualDetails(firstName = "FirstName", lastName = "LastName", middleName = None),
                                      email = "email@email.com",
                                      phone = Some("07111222333"),
                                      mobile = None
      )
    )
  )

  val primaryContact: PrimaryContact = PrimaryContact(
    Seq(
      ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Organisation Name"),
                                        email = "email@email.com",
                                        phone = None,
                                        mobile = None
      )
    )
  )

  val secondaryContact: SecondaryContact = SecondaryContact(
    Seq(
      ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Secondary contact name"),
                                        email = "email2@email.com",
                                        phone = Some("07111222333"),
                                        mobile = None
      )
    )
  )

  val responseCommon: ResponseCommon = ResponseCommon(status = "OK", statusText = None, processingDate = "2020-08-09T11:23:45Z", returnParameters = None)

  val responseDetail: ResponseDetail = ResponseDetail(subscriptionID = "XE0001234567890",
                                                      tradingName = Some("Trading Name"),
                                                      isGBUser = true,
                                                      primaryContact = primaryContact,
                                                      secondaryContact = Some(secondaryContact)
  )

  val enrolmentID: String = "1234567890"

  val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
    DisplaySubscriptionForDACResponse(
      SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetail)
    )

  val requestCommon: RequestCommonForUpdate = RequestCommonForUpdate(
    regime = "DAC",
    receiptDate = "2020-09-23T16:12:11Z",
    acknowledgementReference = "AB123c",
    originatingSystem = "MDTP",
    requestParameters = None
  )

  val primaryContactForInd: PrimaryContact = PrimaryContact(
    Seq(ContactInformationForIndividual(IndividualDetails("FirstName", "LastName", None), "email@email.com", None, None))
  )

  val requestDetailForUpdate: RequestDetailForUpdate = RequestDetailForUpdate(
    IDType = "DAC",
    IDNumber = "IDNumber",
    tradingName = None,
    isGBUser = false,
    primaryContact = primaryContactForInd,
    secondaryContact = None
  )

  val mockHttpClient: HttpClient = mock[HttpClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.cross-border-arrangements.port" -> server.port()
    )
    .build()

  lazy val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  "SubscriptionConnector" - {

    "displaySubscriptionDetails" - {

      "must return the correct DisplaySubscriptionForDACResponse for an Individual" in {
        forAll(validDacID) {
          safeID =>
            val expectedBody = displaySubscriptionPayloadNoSecondary(JsString(safeID),
                                                                     JsString("FirstName"),
                                                                     JsString("LastName"),
                                                                     JsString("email@email.com"),
                                                                     JsString("07111222333")
            )

            val responseDetailUpdate: ResponseDetail =
              responseDetail.copy(subscriptionID = safeID, primaryContact = individualPrimaryContact, secondaryContact = None)

            val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
              )

            stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", OK, expectedBody)

            val result = connector.displaySubscriptionDetails(enrolmentID)
            result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionForDACResponse))
        }
      }

      "must return the correct DisplaySubscriptionForDACResponse for an Organisation" in {
        forAll(validDacID) {
          safeID =>
            val expectedBody = displaySubscriptionPayload(
              JsString(safeID),
              JsString("Organisation Name"),
              JsString("Secondary contact name"),
              JsString("email@email.com"),
              JsString("email2@email.com"),
              JsString("07111222333")
            )

            val responseDetailUpdate: ResponseDetail = responseDetail.copy(subscriptionID = safeID)

            val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
              )

            stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", OK, expectedBody)

            val result = connector.displaySubscriptionDetails(enrolmentID)
            result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionForDACResponse))
        }
      }

      "must return None if unable to validate json" in {
        forAll(validDacID) {
          safeID =>
            val invalidBody =
              s"""
                 |{
                 |  "displaySubscriptionForDACResponse": {
                 |    "responseCommon": {
                 |      "processingDate": "2020-08-09T11:23:45Z"
                 |    },
                 |    "responseDetail": {
                 |      "subscriptionID": "$safeID",
                 |      "tradingName": "Trading Name",
                 |      "isGBUser": true,
                 |      "primaryContact": [
                 |        {
                 |          "email": "email@email.com",
                 |          "individual": {
                 |            "lastName": "LastName",
                 |            "firstName": "FirstName"
                 |          }
                 |        }
                 |      ]
                 |    }
                 |  }
                 |}""".stripMargin

            stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", OK, invalidBody)

            val result = connector.displaySubscriptionDetails(enrolmentID)
            result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(None)
        }
      }

      "must return no contact details and lock as true if 'Create/Amend request is in progress'" in {
        val errorDetail =
          """{
            |"errorDetail": {
            |  "timestamp": "2020-12-31T08:14:07.148Z",
            |  "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
            |  "errorCode": "503",
            |  "errorMessage": "Create/Amend request is in progress",
            |  "source": "Back End",
            |  "sourceFaultDetail": {
            |    "detail": [
            |      "201 - Create/Amend request is in progress"
            |    ]
            |  }
            |}
            |}""".stripMargin

        stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", SERVICE_UNAVAILABLE, errorDetail)

        val result = connector.displaySubscriptionDetails(enrolmentID)
        result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(None, isLocked = true)
      }

      "must return None if status is not OK" in {
        val errorDetail =
          """{
            |"errorDetail": {
            |  "timestamp": "2016-10-10T13:52:16Z",
            |  "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
            |  "errorCode": "503",
            |  "errorMessage": "Request could not be processed",
            |  "source": "Back End",
            |  "sourceFaultDetail": {
            |    "detail": [
            |      "001 - Request could not be processed"
            |    ]
            |  }
            |}
            |}""".stripMargin

        stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", SERVICE_UNAVAILABLE, errorDetail)

        val result = connector.displaySubscriptionDetails(enrolmentID)
        result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(None)
      }

      "must return None if status is not OK and deserialisation failed" in {
        val errorDetail =
          """{
            |"errorDetail": {
            |  "timestamp": "2016-10-10T13:52:16Z",
            |  "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
            |  "errorCode": "503",
            |  "errorMessage": "Request could not be processed",
            |  "source": "Back End"
            |}
            |}""".stripMargin

        stubResponse("/disclose-cross-border-arrangements/subscription/retrieve-subscription", SERVICE_UNAVAILABLE, errorDetail)

        val result = connector.displaySubscriptionDetails(enrolmentID)
        result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(None)
      }
    }

    "updateSubscription" - {

      "must return UpdateSubscriptionForDACResponse if status is OK and users updated their contact info" in {
        val dacID = validDacID.toString

        val returnParameters: ReturnParameters   = ReturnParameters("Name", "Value")
        val responseDetailUpdate: ResponseDetail = responseDetail.copy(subscriptionID = dacID)

        val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
          DisplaySubscriptionForDACResponse(
            SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
          )

        val updateSubscriptionForDACResponse: UpdateSubscriptionForDACResponse =
          UpdateSubscriptionForDACResponse(
            UpdateSubscription(responseCommon = ResponseCommon("OK", None, "2020-09-23T16:12:11Z", Some(Seq(returnParameters))),
                               responseDetail = ResponseDetailForUpdate(dacID)
            )
          )

        stubResponse("/disclose-cross-border-arrangements/subscription/update-subscription", OK, updateSubscriptionResponsePayload(JsString(dacID)))

        val userAnswers = UserAnswers(userAnswersId)
          .set(ContactNamePage, "Organisation Name")
          .success
          .value

        val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, userAnswers)

        result.futureValue mustBe Some(updateSubscriptionForDACResponse)

      }

      "must return None if unable to validate json" in {
        forAll(validDacID) {
          safeID =>
            val invalidBody =
              s"""
                |{
                |  "updateSubscriptionForDACResponse": {
                |    "responseCommon": {
                |      "status": "OK"
                |    },
                |    "responseDetail": {
                |      "subscriptionID": "$safeID"
                |    }
                |  }
                |}
                |""".stripMargin

            stubResponse("/disclose-cross-border-arrangements/subscription/update-subscription", OK, invalidBody)

            val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, emptyUserAnswers)
            result.futureValue mustBe None
        }
      }

      "must return None if status is not OK" in {
        stubResponse("/disclose-cross-border-arrangements/subscription/update-subscription", SERVICE_UNAVAILABLE)

        val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, emptyUserAnswers)
        result.futureValue mustBe None
      }
    }

    "cacheSubscription" - {
      "must return OK if update was stored successfully" in {
        stubResponse("/disclose-cross-border-arrangements/subscription/update-cache-subscription", OK)

        val updateSubscriptionDetails = UpdateSubscriptionDetails(requestCommon, requestDetailForUpdate)

        val result = connector.cacheSubscription(updateSubscriptionDetails, "subscriptionID")
        result.futureValue.status mustBe OK
      }
    }
  }

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
