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

package controllers

import java.util.UUID

import connectors.CrossBorderArrangementsConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import javax.inject.Inject
import models.ManualSubmissionValidationResult
import navigation.Navigator
import org.slf4j.LoggerFactory
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{ManualSubmissionValidationEngine, ValidationEngine}
import uk.gov.hmrc.http.HeaderNames.xSessionId
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.{Elem, NodeSeq}

class ManualSubmissionValidationController @Inject()(
                                                  //    messagesApi: MessagesApi,
                                                     identify: IdentifierAction,
                                                  //   getData: DataRetrievalAction,
                                                   //   val sessionRepository: SessionRepository,
                                                      val controllerComponents: MessagesControllerComponents,
                                                  //    connector: CrossBorderArrangementsConnector,
                                                  //    requireData: DataRequiredAction,
                                                        validationEngine: ManualSubmissionValidationEngine ,
                                                  //    errorHandler: ErrorHandler,
                                                  //    navigator: Navigator
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  def validateManualSubmission: Action[NodeSeq] =  (identify /*andThen getData*/).async(parse.xml) {
    implicit request => {

      validationEngine.validateManualSubmission(request.body, request.enrolmentID) map {

        case Some(result) => Ok(Json.toJson(ManualSubmissionValidationResult(result)))
        case None => BadRequest("Invalid_XML")
      }
    }

  }
  private def convertToResult(httpResponse: HttpResponse): Result = {
    httpResponse.status match {
      case OK => Ok(httpResponse.body)

      case NOT_FOUND => NotFound(httpResponse.body)

      case BAD_REQUEST => BadRequest(httpResponse.body)

      case FORBIDDEN => Forbidden(httpResponse.body)

      case METHOD_NOT_ALLOWED => MethodNotAllowed(httpResponse.body)

      case CONFLICT => Conflict(httpResponse.body)

      case INTERNAL_SERVER_ERROR => InternalServerError(httpResponse.body)

      case _ => ServiceUnavailable(httpResponse.body)
    }
  }



}
