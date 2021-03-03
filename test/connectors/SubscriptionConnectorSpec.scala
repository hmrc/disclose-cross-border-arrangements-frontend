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
import controllers.Assets.SERVICE_UNAVAILABLE
import generators.Generators
import helpers.JsonFixtures._
import models.UserAnswers
import models.subscription._
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.contactdetails.ContactNamePage
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, JsValue}
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionConnectorSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with Generators with BeforeAndAfterEach {

  val individualPrimaryContact: PrimaryContact = PrimaryContact(Seq(
    ContactInformationForIndividual(
      individual = IndividualDetails(firstName = "FirstName", lastName = "LastName", middleName = None),
      email = "email@email.com", phone = Some("07111222333"), mobile = None)
  ))

  val primaryContact: PrimaryContact = PrimaryContact(Seq(
    ContactInformationForOrganisation(
      organisation = OrganisationDetails(organisationName = "Organisation Name"),
      email = "email@email.com", phone = None, mobile = None)
  ))

  val secondaryContact: SecondaryContact = SecondaryContact(Seq(
    ContactInformationForOrganisation(
      organisation = OrganisationDetails(organisationName = "Secondary contact name"),
      email = "email2@email.com", phone = Some("07111222333"), mobile = None)
  ))

  val responseCommon: ResponseCommon = ResponseCommon(
    status = "OK",
    statusText = None,
    processingDate = "2020-08-09T11:23:45Z",
    returnParameters = None)

  val responseDetail: ResponseDetail = ResponseDetail(
    subscriptionID = "XE0001234567890",
    tradingName = Some("Trading Name"),
    isGBUser = true,
    primaryContact = primaryContact,
    secondaryContact = Some(secondaryContact))

  val enrolmentID: String = "1234567890"

  val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
    DisplaySubscriptionForDACResponse(
      SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetail)
    )

  val mockHttpClient: HttpClient = mock[HttpClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[HttpClient].toInstance(mockHttpClient)
    ).build()

  lazy val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  override def beforeEach {
    Mockito.reset(mockHttpClient)
  }

  "SubscriptionConnector" - {

    "displaySubscriptionDetails" - {

      "must return the correct DisplaySubscriptionForDACResponse for an Individual" in {
        forAll(validDacID) {
          safeID =>
            val expectedBody = displaySubscriptionPayloadNoSecondary(
              JsString(safeID), JsString("FirstName"), JsString("LastName"), JsString("email@email.com"), JsString("07111222333"))

            val responseDetailUpdate: ResponseDetail = responseDetail.copy(
              subscriptionID = safeID, primaryContact = individualPrimaryContact, secondaryContact = None)

            val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
              )

            when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, expectedBody)))


            val result = connector.displaySubscriptionDetails(enrolmentID)
            result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(Some(displaySubscriptionForDACResponse))
        }
      }

      "must return the correct DisplaySubscriptionForDACResponse for an Organisation" in {
        forAll(validDacID) {
          safeID =>
            val expectedBody = displaySubscriptionPayload(
              JsString(safeID), JsString("Organisation Name"), JsString("Secondary contact name"),
              JsString("email@email.com"), JsString("email2@email.com"), JsString("07111222333"))

            val responseDetailUpdate: ResponseDetail = responseDetail.copy(subscriptionID = safeID)

            val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
              )

            when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, expectedBody)))


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

            when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, invalidBody)))

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

        when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, errorDetail)))

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

        when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, errorDetail)))

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

        when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, errorDetail)))

        val result = connector.displaySubscriptionDetails(enrolmentID)
        result.futureValue mustBe DisplaySubscriptionDetailsAndStatus(None)
      }
    }

    "updateSubscription" - {

      "must return UpdateSubscriptionForDACResponse if status is OK and users updated their contact info" in {
       val dacID = validDacID.toString

            val returnParameters: ReturnParameters = ReturnParameters("Name", "Value")
            val responseDetailUpdate: ResponseDetail = responseDetail.copy(subscriptionID = dacID)

            val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse =
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetailUpdate)
              )

            val updateSubscriptionForDACResponse: UpdateSubscriptionForDACResponse =
              UpdateSubscriptionForDACResponse(
                UpdateSubscription(
                  responseCommon = ResponseCommon("OK", None, "2020-09-23T16:12:11Z", Some(Seq(returnParameters))),
                  responseDetail = ResponseDetailForUpdate(dacID)))

            when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, updateSubscriptionResponsePayload(JsString(dacID)))))

            val userAnswers = UserAnswers(userAnswersId)
              .set(ContactNamePage, "Organisation Name").success.value


            val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, userAnswers)
            val argumentCaptor: ArgumentCaptor[UpdateSubscriptionForDACRequest] = ArgumentCaptor.forClass(classOf[UpdateSubscriptionForDACRequest])

            verify(mockHttpClient, times(1)).POST(any(), argumentCaptor.capture(), any())(any(), any(), any(), any())

            result.futureValue mustBe Some(updateSubscriptionForDACResponse)

            val body = argumentCaptor.getValue
            body.updateSubscriptionForDACRequest.requestDetail.IDType mustBe "DAC"

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

            when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, invalidBody)))

            val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, emptyUserAnswers)
            result.futureValue mustBe None
        }
      }

      "must return None if status is not OK" in {
        when(mockHttpClient.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))

        val result = connector.updateSubscription(displaySubscriptionForDACResponse.displaySubscriptionForDACResponse, emptyUserAnswers)
        result.futureValue mustBe None
      }
    }
  }

}
