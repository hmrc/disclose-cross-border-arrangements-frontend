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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UpscanConnector
import javax.inject.Singleton
import models.upscan.{UploadId, UpscanInitiateRequest}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.UploadProgressTracker
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadFormController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      val controllerComponents: MessagesControllerComponents,
                                      appConfig: FrontendAppConfig,
                                      upscanInitiateConnector: UpscanConnector,
                                      uploadProgressTracker: UploadProgressTracker,
                                      renderer: Renderer
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad: Action[AnyContent] = Action.async  {
    implicit request =>

      val uploadId           = UploadId.generate
      val successRedirectUrl = appConfig.upscanRedirectBase +  routes.UploadFormController.showResult(uploadId).url
      val errorRedirectUrl   = appConfig.upscanRedirectBase + "/disclose-cross-border-arrangements/error"
      val callbackUrl = controllers.routes.UploadCallbackController.callback().absoluteURL(appConfig.upscanProtocol == "https")
      val initiateBody = UpscanInitiateRequest(callbackUrl, successRedirectUrl, errorRedirectUrl)

      {
        for {
          upscanInitiateResponse <- upscanInitiateConnector.getUpscanFormData(initiateBody)
          _                      <- uploadProgressTracker.requestUpload(uploadId,   upscanInitiateResponse.fileReference)
        } yield {
          renderer.render(
            "upload-form.njk",
            Json.obj("upscanInitiateResponse" -> Json.toJson(upscanInitiateResponse))
          ).map(Ok(_))
        }
      }.flatMap(identity)
  }

  def showResult(uploadId: UploadId): Action[AnyContent] = Action.async {
    implicit request => {
      Logger.debug("Show result called")
      for (uploadResult <- uploadProgressTracker.getUploadResult(uploadId)) yield {
        {
          uploadResult match {
            case Some(result) =>
              renderer.render(
                "upload-result.njk",
                Json.obj("uploadId" -> Json.toJson(uploadId),
                  "status" -> Json.toJson(result))
              ).map(Ok(_))
            case None => Future.successful(BadRequest(s"Upload with id $uploadId not found"))
          }
        }
      }
    }.flatMap(identity)
  }

  def showError(errorCode: String, errorMessage: String, errorRequestId: String): Action[AnyContent] = Action.async {
    implicit request => errorCode match {
      case "EntityTooLarge" =>
        renderer.render(
          "fileTooLargeError.njk",
          Json.obj("guidanceLink" -> Json.toJson(appConfig.xmlTechnicialGuidanceUrl))
        ).map (Ok (_))
      case _ =>
          renderer.render (
          "upload-error.njk",
          Json.obj ("pageTitle" -> "Upload Error",
          "heading" -> errorMessage,
          "message" -> s"Code: $errorCode, RequestId: $errorRequestId")
          ).map (Ok (_) )
    }
  }

}
