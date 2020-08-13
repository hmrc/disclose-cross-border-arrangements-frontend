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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import controllers.routes
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.mvc.{Action, AnyContent, BodyParsers, MessagesControllerComponents, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  private implicit class HelperOps[A](a: A) {
    def ~[B](b: B) = new ~(a, b)
  }

  lazy val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val bodyParsers: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  object Harness {
    sealed class Harness(authAction: IdentifierAction, controllerComponents: MessagesControllerComponents = mockMcc)
      extends FrontendController(controllerComponents) {
      def onPageLoad(): Action[AnyContent] = authAction { request => Results.Ok(s"Identifier: ${request.identifier}, EnrolmentID: ${request.enrolmentID}") }
    }


    def fromAction(action: IdentifierAction): Harness =
      new Harness(action)

    def failure(ex: Throwable): Harness =
      fromAction(new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(ex), frontendAppConfig, bodyParsers))

    def successful[A](a: A): Harness = {
      val mocked = mock[AuthConnector]
      when(mocked.authorise[A](any(), any())(any(), any())).thenReturn(Future.successful(a))
      fromAction(new AuthenticatedIdentifierAction(mocked, frontendAppConfig, bodyParsers))
    }
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {
        val controller = Harness.failure(new MissingBearerToken)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "when the user's session has expired" - {

      "must redirect the user to log in " in {
        val controller = Harness.failure(new BearerTokenExpired)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "when the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {
        val controller = Harness.failure(new InsufficientEnrolments)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "when the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {
        val controller = Harness.failure(new InsufficientConfidenceLevel)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "when the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {
        val controller = Harness.failure(new UnsupportedAuthProvider)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "when the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {
        val controller = Harness.failure(new UnsupportedAffinityGroup)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "when the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {
        val controller = Harness.failure(new UnsupportedCredentialRole)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "when the user has the correct DAC6 enrolment" - {
      "must extract the enrolmentID correctly" in {
        val retrievals = Some("internalID") ~ Enrolments(Set(Enrolment("DAC6", Seq(EnrolmentIdentifier("EnrolmentID", "thisismyenrolmentID")), "ACTIVE")))

        val controller = Harness.successful(retrievals)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe "Identifier: internalID, EnrolmentID: thisismyenrolmentID"
      }
    }
  }
}



class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
