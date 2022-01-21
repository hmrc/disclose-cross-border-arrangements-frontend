/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.UpscanConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.exceptions.UpscanTimeoutException
import forms.mappings.Mappings
import handlers.ErrorHandler
import models.UserAnswers
import models.requests.OptionalDataRequest
import models.upscan._
import org.slf4j.LoggerFactory
import pages.UploadIDPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  appConfig: FrontendAppConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  upscanConnector: UpscanConnector,
  sessionRepository: SessionRepository,
  renderer: Renderer,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  private[controllers] def form: Form[String] = new Mappings {
    val apply: Form[String] = Form("file" -> text())
  }.apply

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      toResponse(form)
  }

  private[controllers] def toResponse(preparedForm: Form[String])(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {

    def json(upscanInitiateResponse: UpscanInitiateResponse): JsObject =
      Json.obj("form" -> preparedForm, "upscanInitiateResponse" -> Json.toJson(upscanInitiateResponse), "status" -> Json.toJson(0))

    (for {
      upscanInitiateResponse <- upscanConnector.getUpscanFormData
      uploadId               <- upscanConnector.requestUpload(upscanInitiateResponse.fileReference)
      updatedAnswers         <- Future.fromTry(UserAnswers(request.internalId).set(UploadIDPage, uploadId))
      _                      <- sessionRepository.set(updatedAnswers)
      html                   <- renderer.render("upload-form.njk", json(upscanInitiateResponse))
    } yield html)
      .recover {
        case _: Exception => throw new UpscanTimeoutException
      }
      .map(Ok(_))
  }

  def showResult: Action[AnyContent] = Action.async {
    implicit uploadResponse =>
      renderer.render("upload-result.njk").map(Ok(_))
  }

  def showError(errorCode: String, errorMessage: String, errorRequestId: String): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      errorCode match {
        case "EntityTooLarge" =>
          renderer
            .render(
              "fileTooLargeError.njk",
              Json.obj("xmlTechnicalGuidanceUrl" -> Json.toJson(appConfig.xmlTechnicalGuidanceUrl))
            )
            .map(Ok(_))
        case "InvalidArgument" =>
          val formWithErrors: Form[String] = form.withError("file", "upload_form.error.file.empty")
          toResponse(formWithErrors)
        case _ =>
          logger.error(s"Upscan error $errorCode: $errorMessage, requestId is $errorRequestId")
          renderer.render("serviceError.njk").map(InternalServerError(_))
      }
  }

  def getStatus: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      logger.debug("Show status called")
      request.userAnswers.flatMap(_.get(UploadIDPage)) match {
        case Some(uploadId) =>
          upscanConnector.getUploadStatus(uploadId) flatMap {
            case Some(_: UploadedSuccessfully) =>
              Future.successful(Redirect(routes.FileValidationController.onPageLoad()))
            case Some(r: UploadRejected) =>
              val errorMessage = if (r.details.message.contains("octet-stream")) {
                "upload_form.error.file.empty"
              } else {
                "upload_form.error.file.invalid"
              }
              val errorForm: Form[String] = form.withError("file", errorMessage)
              logger.debug(s"Show errorForm on rejection $errorForm")
              toResponse(errorForm)
            case Some(Quarantined) =>
              Future.successful(Redirect(routes.VirusErrorController.onPageLoad()))
            case Some(Failed) =>
              renderer.render("serviceError.njk").map(InternalServerError(_))
            case Some(_) =>
              renderer.render("upload-result.njk").map(Ok(_))
            case None =>
              renderer.render("serviceError.njk").map(InternalServerError(_))
          }
        case None =>
          renderer.render("serviceError.njk").map(InternalServerError(_))
      }
  }

}
