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

package controllers.actions

import config.FrontendAppConfig
import connectors.SubscriptionConnector
import models.ContactDetails
import models.requests.{DataRequest, DataRequestWithContacts}
import models.subscription.{ContactInformationForIndividual, ContactInformationForOrganisation, DisplaySubscriptionForDACResponse}
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactRetrievalActionImpl @Inject() (frontendAppConfig: FrontendAppConfig, subscriptionConnector: SubscriptionConnector)(implicit
  val executionContext: ExecutionContext
) extends ContactRetrievalAction {

  override protected def transform[A](request: DataRequest[A]): Future[DataRequestWithContacts[A]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (!frontendAppConfig.sendEmailToggle) {
      Future.successful(DataRequestWithContacts(request.request, request.internalId, request.enrolmentID, request.userAnswers, None))
    } else {
      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).map {
        details =>
          val contacts = details.subscriptionDetails.map {
            details =>
              buildContactDetails(details)
          }

          DataRequestWithContacts(request.request, request.internalId, request.enrolmentID, request.userAnswers, contacts)
      }
    }
  }

  private def buildContactDetails(details: DisplaySubscriptionForDACResponse): ContactDetails = {
    val responseDetail = details.displaySubscriptionForDACResponse.responseDetail

    val (contactName, primaryEmail) = responseDetail.primaryContact.contactInformation.head match {
      case ContactInformationForIndividual(individual, email, _, _) =>
        (s"${individual.firstName} ${individual.middleName.fold("")(
           mn => s"$mn "
         )}${individual.lastName}",
         email
        )
      case ContactInformationForOrganisation(organisation, email, _, _) =>
        (s"${organisation.organisationName}", email)
    }

    val secondaryContact =
      responseDetail.secondaryContact.map {
        secondaryContact =>
          secondaryContact.contactInformation.head match {
            case ContactInformationForIndividual(individual, email, _, _) =>
              (s"${individual.firstName} ${individual.middleName.fold("")(
                 mn => s"$mn "
               )}${individual.lastName}",
               email
              )
            case ContactInformationForOrganisation(organisation, email, _, _) =>
              (s"${organisation.organisationName}", email)
          }
      }

    if (secondaryContact.isDefined) {
      ContactDetails(
        contactName = Some(contactName),
        contactEmail = Some(primaryEmail),
        secondContactName = Some(secondaryContact.get._1),
        secondEmail = Some(secondaryContact.get._2)
      )
    } else {
      ContactDetails(
        contactName = Some(contactName),
        contactEmail = Some(primaryEmail),
        secondContactName = None,
        secondEmail = None
      )
    }
  }

}

trait ContactRetrievalAction extends ActionTransformer[DataRequest, DataRequestWithContacts]
