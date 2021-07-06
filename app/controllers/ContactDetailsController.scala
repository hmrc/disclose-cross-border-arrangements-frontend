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

import config.FrontendAppConfig
import connectors.SubscriptionConnector
import controllers.actions._
import handlers.ErrorHandler
import helpers.ViewHelper
import models.UserAnswers
import models.subscription.{ResponseDetail, UpdateSubscriptionDetails}
import pages.DisplaySubscriptionDetailsPage
import pages.contactdetails._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.SummaryList

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ContactDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  subscriptionConnector: SubscriptionConnector,
  appConfig: FrontendAppConfig,
  errorHandler: ErrorHandler,
  viewHelper: ViewHelper,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  renderer: Renderer
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).flatMap {
        details =>
          if (details.isLocked) {
            Future.successful(Redirect(routes.DetailsAlreadyUpdatedController.onPageLoad()))
          } else {
            if (details.subscriptionDetails.isDefined) {
              val responseDetail = details.subscriptionDetails.get.displaySubscriptionForDACResponse.responseDetail
              val isOrganisation = viewHelper.isOrganisation(responseDetail.primaryContact.contactInformation)

              val json = {
                if (isOrganisation) {
                  Json.obj(
                    "contactDetails"          -> buildPrimaryContactRows(responseDetail, request.userAnswers),
                    "additionalContact"       -> true,
                    "secondaryContactDetails" -> buildSecondaryContactRows(responseDetail, request.userAnswers),
                    "isOrganisation"          -> isOrganisation,
                    "homePageLink"            -> appConfig.discloseArrangeLink,
                    "changeProvided"          -> changeProvided(request.userAnswers)
                  )
                } else {
                  Json.obj(
                    "contactDetails"    -> buildPrimaryContactRows(responseDetail, request.userAnswers),
                    "additionalContact" -> false,
                    "isOrganisation"    -> isOrganisation,
                    "homePageLink"      -> appConfig.discloseArrangeLink,
                    "changeProvided"    -> changeProvided(request.userAnswers)
                  )
                }
              }

              (for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(DisplaySubscriptionDetailsPage, details.subscriptionDetails.get))
                _              <- sessionRepository.set(updatedAnswers)
              } yield renderer.render("contactDetails.njk", json).map(Ok(_))).flatten
            } else {
              errorHandler.onServerError(request, new Exception("Conversion of display subscription payload failed"))
            }
          }
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).flatMap {
        detailsAndStatus =>
          detailsAndStatus.subscriptionDetails match {
            case Some(details) =>
              val subscriptionDetails = details.displaySubscriptionForDACResponse

              subscriptionConnector.updateSubscription(subscriptionDetails, request.userAnswers).flatMap {
                case Some(updateResponse) =>
                  subscriptionConnector
                    .cacheSubscription(
                      UpdateSubscriptionDetails.updateSubscription(subscriptionDetails, request.userAnswers),
                      updateResponse.updateSubscriptionForDACResponse.responseDetail.subscriptionID
                    )
                    .flatMap {
                      _ =>
                        for {
                          updatedUserAnswers <- Future.fromTry(cleanupAnswers(request.userAnswers))
                          _                  <- sessionRepository.set(updatedUserAnswers)
                        } yield Redirect(routes.DetailsUpdatedController.onPageLoad())
                    }
                case None => Future.successful(Redirect(routes.DetailsNotUpdatedController.onPageLoad()))
              }
            case None => Future.successful(Redirect(routes.DetailsNotUpdatedController.onPageLoad()))
          }
      } recover {
        case _ => Redirect(routes.DetailsNotUpdatedController.onPageLoad())
      }
  }

  private def buildPrimaryContactRows(responseDetail: ResponseDetail, userAnswers: UserAnswers): Seq[SummaryList.Row] =
    Seq(
      viewHelper.primaryContactName(responseDetail, userAnswers),
      Some(viewHelper.primaryContactEmail(responseDetail, userAnswers)),
      Some(viewHelper.haveContactPhoneNumber(responseDetail, userAnswers)),
      viewHelper.primaryPhoneNumber(responseDetail, userAnswers)
    ).filter(_.isDefined).map(_.get)

  private def buildSecondaryContactRows(responseDetail: ResponseDetail, userAnswers: UserAnswers): Seq[SummaryList.Row] =
    userAnswers.get(HaveSecondContactPage) match {
      case Some(false) =>
        Seq(viewHelper.haveSecondaryContact(responseDetail, userAnswers))
      case haveSecondContact: Option[Boolean] =>
        if (haveSecondContact.isDefined || responseDetail.secondaryContact.isDefined) {
          Seq(
            Some(viewHelper.haveSecondaryContact(responseDetail, userAnswers)),
            Some(viewHelper.secondaryContactName(responseDetail, userAnswers)),
            Some(viewHelper.secondaryContactEmail(responseDetail, userAnswers)),
            Some(viewHelper.haveSecondaryContactPhone(responseDetail, userAnswers)),
            viewHelper.secondaryPhoneNumber(responseDetail, userAnswers)
          ).filter(_.isDefined).map(_.get)
        } else if (responseDetail.secondaryContact.isEmpty) {
          Seq(viewHelper.haveSecondaryContact(responseDetail, userAnswers))
        } else {
          Seq()
        }
    }

  private def changeProvided(userAnswers: UserAnswers): Boolean =
    List(
      userAnswers.get(ContactNamePage).isDefined,
      userAnswers.get(ContactEmailAddressPage).isDefined,
      userAnswers.get(HaveContactPhonePage).isDefined,
      userAnswers.get(ContactTelephoneNumberPage).isDefined,
      userAnswers.get(HaveSecondContactPage).isDefined,
      userAnswers.get(SecondaryContactNamePage).isDefined,
      userAnswers.get(SecondaryContactEmailAddressPage).isDefined,
      userAnswers.get(HaveSecondaryContactPhonePage).isDefined,
      userAnswers.get(SecondaryContactTelephoneNumberPage).isDefined
    ).contains(true)

  private def cleanupAnswers(userAnswers: UserAnswers): Try[UserAnswers] =
    userAnswers
      .remove(ContactNamePage)
      .flatMap(_.remove(ContactEmailAddressPage))
      .flatMap(_.remove(HaveContactPhonePage))
      .flatMap(_.remove(ContactTelephoneNumberPage))
      .flatMap(_.remove(HaveSecondContactPage))
      .flatMap(_.remove(SecondaryContactNamePage))
      .flatMap(_.remove(SecondaryContactEmailAddressPage))
      .flatMap(_.remove(HaveSecondaryContactPhonePage))
      .flatMap(_.remove(SecondaryContactTelephoneNumberPage))

}
