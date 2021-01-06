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


import controllers.actions.IdentifierAction
import javax.inject.Inject
import models.{ManualSubmissionValidationFailure, ManualSubmissionValidationSuccess}

import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, MessagesControllerComponents}
import services.ManualSubmissionValidationEngine

import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class ManualSubmissionValidationController @Inject()(identify: IdentifierAction,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     validationEngine: ManualSubmissionValidationEngine ,
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def validateManualSubmission: Action[NodeSeq] =  identify.async(parse.xml) {
    implicit request => {

      validationEngine.validateManualSubmission(request.body, request.enrolmentID) map {

        case Some(ManualSubmissionValidationSuccess(messageRefId)) => Ok(Json.toJson(ManualSubmissionValidationSuccess(messageRefId)))
        case Some(ManualSubmissionValidationFailure(Seq(errors))) => Ok(Json.toJson(ManualSubmissionValidationFailure(Seq(errors))))
        case None => BadRequest("Invalid_XML")
      }
    }

  }
}
