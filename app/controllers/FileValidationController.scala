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

import connectors.{UpscanConnector, ValidationConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.upscan.{UploadId, UploadSessionDetails, UploadedSuccessfully}
import models.{Dac6MetaData, GenericError, NormalMode, UserAnswers, ValidationErrors}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileValidationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  auditService: AuditService,
  val sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  upscanConnector: UpscanConnector,
  requireData: DataRequiredAction,
  validationConnector: ValidationConnector,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      {
        for {
          uploadId       <- getUploadId(request.userAnswers)
          uploadSessions <- upscanConnector.getUploadDetails(uploadId)
          (fileName, upScanUrl) = getDownloadUrl(uploadSessions)
          validation: Option[Either[ValidationErrors, Dac6MetaData]] <- validationConnector.sendForValidation(upScanUrl)
        } yield validation match {
          case Some(Right(metaData)) =>
            for {
              updatedAnswers             <- Future.fromTry(UserAnswers(request.internalId).set(ValidXMLPage, fileName))
              updatedAnswersWithURL      <- Future.fromTry(updatedAnswers.set(URLPage, upScanUrl))
              updatedAnswersWithMetaData <- Future.fromTry(updatedAnswersWithURL.set(Dac6MetaDataPage, metaData))
              _                          <- sessionRepository.set(updatedAnswersWithMetaData)
            } yield metaData.importInstruction match {
              case "DAC6DEL" => Redirect(routes.DeleteDisclosureSummaryController.onPageLoad())
              case _         => Redirect(navigator.nextPage(ValidXMLPage, NormalMode, updatedAnswers))
            }

          case Some(Left(ValidationErrors(errors, dac6MetaData))) =>
            for {
              updatedAnswers           <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              updatedAnswersWithErrors <- Future.fromTry(updatedAnswers.set(GenericErrorPage, errors))
              _                        <- sessionRepository.set(updatedAnswersWithErrors)
            } yield {
              errors.foreach(auditService.auditErrorMessage(_))
              Redirect(navigator.nextPage(InvalidXMLPage, NormalMode, updatedAnswers))
            }

          case _ =>
            for {
              updatedAnswers <- Future.fromTry(UserAnswers(request.internalId).set(InvalidXMLPage, fileName))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(routes.FileErrorController.onPageLoad())
        }
      }.flatten
  }

  private def getUploadId(userAnswers: UserAnswers): Future[UploadId] =
    userAnswers.get(UploadIDPage) match {
      case Some(uploadId) => Future.successful(uploadId)
      case None           => throw new RuntimeException("Cannot find uploadId")
    }

  private def getDownloadUrl(uploadSessions: Option[UploadSessionDetails]) =
    uploadSessions match {
      case Some(uploadDetails) =>
        uploadDetails.status match {
          case UploadedSuccessfully(name, downloadUrl) => (name, downloadUrl)
          case _                                       => throw new RuntimeException("File not uploaded successfully")
        }
      case _ => throw new RuntimeException("File not uploaded successfully")
    }
}
