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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import javax.inject.Inject
import models.upscan.{UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{NormalMode, UserAnswers, ValidationFailure, ValidationSuccess}
import navigation.Navigator
import pages.{Dac6MetaDataPage, InvalidXMLPage, URLPage, UploadIDPage, ValidXMLPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{SessionRepository, UploadSessionRepository}
import services.ValidationEngine
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class FileValidationController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  repository: UploadSessionRepository,
  requireData: DataRequiredAction,
  validationEngine: ValidationEngine ,
  errorHandler: ErrorHandler,
  navigator: Navigator
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async{
    implicit request =>
    {
      for {
        uploadId <- getUploadId(request.userAnswers)
        uploadSessions <- repository.findByUploadId(uploadId)
        (fileName, downloadUrl) = getDownloadUrl(uploadSessions)
        validation = validationEngine.validateFile(downloadUrl)
      } yield {
        validation flatMap   {
          case ValidationSuccess(_,Some(metaData)) =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(ValidXMLPage, fileName))
              updatedAnswersWithURL <- Future.fromTry(updatedAnswers.set(URLPage, downloadUrl))
              updatedAnswersWithMetaData <- Future.fromTry(updatedAnswersWithURL.set(Dac6MetaDataPage, metaData))
              _              <- sessionRepository.set(updatedAnswersWithMetaData)
            } yield {
              metaData.importInstruction match {
                case "DAC6DEL" => Redirect(routes.DeleteDisclosureSummaryController.onPageLoad())
                case _ => Redirect(navigator.nextPage(ValidXMLPage, NormalMode, updatedAnswers))
              }
            }
          case ValidationFailure(_) =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(navigator.nextPage(InvalidXMLPage, NormalMode, updatedAnswers))
            }
          case _ =>
            errorHandler.onServerError(request, throw new RuntimeException("file validation failed - missing data"))
        }
      }
    }.flatten
  }

  private def getUploadId(userAnswers: UserAnswers): Future[UploadId] = {
    userAnswers.get(UploadIDPage) match {
      case Some(uploadId) => Future.successful(uploadId)
      case None => throw new RuntimeException("Cannot find uploadId")
    }
  }

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

