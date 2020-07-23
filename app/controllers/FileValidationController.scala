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

package controllers

import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import javax.inject.Inject
import models.upscan.{UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{NormalMode, UserAnswers, ValidationFailure, ValidationSuccess}
import navigation.Navigator
import pages.{InvalidUploadIDPage, InvalidXMLPage, URLPage, ValidUploadIDPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import repositories.{SessionRepository, UploadSessionRepository}
import services.XMLValidationService
import services.UploadProgressTracker
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class FileValidationController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          val sessionRepository: SessionRepository,
                                          val controllerComponents: MessagesControllerComponents,
                                          appConfig: FrontendAppConfig,
                                          repository: UploadSessionRepository,
                                          requireData: DataRequiredAction,
                                          service: XMLValidationService,
                                          uploadProgressTracker: UploadProgressTracker,
                                          renderer: Renderer,
                                          navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen  requireData ).async { implicit request =>
    {
      request.userAnswers.get(ValidUploadIDPage)match {
        case Some(uploadID) =>   {

      for {
        uploadSessions <- repository.findByUploadId(UploadId(uploadID))
        (fileName, downloadUrl) = getDownloadUrl(uploadSessions)
        validation = service.validateXML(downloadUrl)
      } yield {
        validation match {
          case ValidationSuccess(_) =>
            {for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(ValidUploadIDPage, fileName))
              updatedAnswersWithURL <- Future.fromTry(updatedAnswers.set(URLPage, downloadUrl))
              _              <- sessionRepository.set(updatedAnswersWithURL)
            } yield {
              renderer.render(
                "file-validation.njk",
                Json.obj("validationResult" -> Json.toJson(validation))
              ).map(Ok(_))
            }}.flatten

          case ValidationFailure(_) =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidUploadIDPage, fileName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(navigator.nextPage(InvalidUploadIDPage, NormalMode, updatedAnswers))
            }
        }
      }
    }.flatten
  }
}}
  private def getDownloadUrl(uploadSessions: Option[UploadSessionDetails]) = {
    uploadSessions match {
      case Some(uploadDetails) => uploadDetails.status match {
        case UploadedSuccessfully(name, downloadUrl) => (name, downloadUrl)
        case _ => throw new RuntimeException("File not uploaded successfully")
      }
      case _ => throw new RuntimeException("File not uploaded successfully")
    }
  }

}

