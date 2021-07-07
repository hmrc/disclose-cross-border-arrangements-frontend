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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {

  private val enrolmentKey: String   = "HMRC-DAC6-ORG"
  private val enrolmentIDKey: String = "DAC6ID"

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(Enrolment(enrolmentKey)).retrieve(Retrievals.internalId and Retrievals.authorisedEnrolments) {

      case Some(internalID) ~ Enrolments(enrolments) =>
        val enrolmentID =
          (for {
            enrolment           <- enrolments.find(_.key.equals(enrolmentKey))
            enrolmentIdentifier <- enrolment.getIdentifier(enrolmentIDKey)
          } yield enrolmentIdentifier.value)
            .getOrElse(throw new Exception("EnrolmentID Required for DAC6"))

        block(IdentifierRequest(request, internalID, enrolmentID))
      case None ~ _ => throw new UnauthorizedException("Unable to retrieve internal Id")
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}

class SessionIdentifierAction @Inject() (
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    hc.sessionId match {
      case Some(session) =>
        block(IdentifierRequest(request, session.value, "EnrolmentID"))
      case None =>
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
    }
  }
}
