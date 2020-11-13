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

package models.subscription

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import models.UserAnswers
import pages._
import play.api.libs.json.{Json, OFormat}

import scala.util.Random


//TODO Refactor to use the other model. As long as conversation id is None, play will not include it in json
case class RequestCommonForUpdate(regime: String,
                                  receiptDate: String,
                                  acknowledgementReference: String,
                                  originatingSystem: String,
                                  requestParameters: Option[Seq[RequestParameter]])

object RequestCommonForUpdate {
  implicit val format: OFormat[RequestCommonForUpdate] = Json.format[RequestCommonForUpdate]

  def createRequestCommon: RequestCommonForUpdate = {
    //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val r = new Random()
    val idSize: Int = 1 + r.nextInt(33) //Generate a size between 1 and 32
    val generateAcknowledgementReference: String = r.alphanumeric.take(idSize).mkString

    RequestCommonForUpdate(
      regime = "DAC",
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = generateAcknowledgementReference,
      originatingSystem = "MDTP",
      requestParameters = None
    )
  }
}

case class RequestDetailForUpdate(IDType: String,
                                  IDNumber: String,
                                  tradingName: Option[String],
                                  isGBUser: Boolean,
                                  primaryContact: PrimaryContact,
                                  secondaryContact: Option[SecondaryContact])
object RequestDetailForUpdate {
  implicit val format: OFormat[RequestDetailForUpdate] = Json.format[RequestDetailForUpdate]
}

case class UpdateSubscriptionDetails(requestCommon: RequestCommonForUpdate,
                                     requestDetail: RequestDetailForUpdate)
object UpdateSubscriptionDetails {

  implicit val format: OFormat[UpdateSubscriptionDetails] = Json.format[UpdateSubscriptionDetails]

  private def buildContactInformation(contactInformation: Seq[ContactInformation],
                                      userAnswers: UserAnswers): Either[ContactInformationForIndividual, ContactInformationForOrganisation] = {

    //Note: There should only be one item in contact information
    contactInformation.head match {
      case ContactInformationForIndividual(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(ContactEmailAddressPage) match {
          case Some(email) => email
          case None => email
        }

        val telephone = userAnswers.get(ContactTelephoneNumberPage) match {
          case Some(phone) => Some(phone)
          case None => phone
        }

        val individualDetails = userAnswers.get(IndividualContactNamePage) match {
          case Some(name) => IndividualDetails(name.firstName, name.lastName, None)
          case None => details
        }

        Left(ContactInformationForIndividual(individualDetails, emailAddress, telephone, mobile))
      case ContactInformationForOrganisation(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(ContactEmailAddressPage) match {
          case Some(email) => email
          case None => email
        }

        val telephone = userAnswers.get(ContactTelephoneNumberPage) match {
          case Some(phone) => Some(phone)
          case None => phone
        }

        val organisationDetails = userAnswers.get(ContactNamePage) match {
          case Some(name) => OrganisationDetails(name)
          case None => details
        }

        Right(ContactInformationForOrganisation(organisationDetails, emailAddress, telephone, mobile))
    }
  }

  private def buildSecondaryContactInformation(contactInformation: Seq[ContactInformation],
                                               userAnswers: UserAnswers): ContactInformationForOrganisation = {

    //Note: Secondary contact name is only one field in the registration journey. It's always ContactInformationForOrganisation
    contactInformation.head match {
      case ContactInformationForOrganisation(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(SecondaryContactEmailAddressPage) match {
          case Some(email) => email
          case None => email
        }

        val telephone = userAnswers.get(SecondaryContactTelephoneNumberPage) match {
          case Some(phone) => Some(phone)
          case None => phone
        }

        val organisationDetails = userAnswers.get(SecondaryContactNamePage) match {
          case Some(name) => OrganisationDetails(name)
          case None => details
        }

        ContactInformationForOrganisation(organisationDetails, emailAddress, telephone, mobile)
      case _ => throw new Exception("Unable to build secondary contact")
    }
  }

  private def createRequestDetail(responseDetail: ResponseDetail,
                                  userAnswers: UserAnswers): RequestDetailForUpdate = {
    val primaryContact =
      buildContactInformation(responseDetail.primaryContact.contactInformation, userAnswers) match {
        case Left(contactInformationForIndividual) => PrimaryContact(Seq(contactInformationForIndividual))
        case Right(contactInformationForOrganisation) => PrimaryContact(Seq(contactInformationForOrganisation))
      }

    val secondaryContact: Option[SecondaryContact] =
      if (responseDetail.secondaryContact.isDefined) {
        val contactInformation = buildSecondaryContactInformation(responseDetail.secondaryContact.get.contactInformation, userAnswers)
        Some(SecondaryContact(Seq(contactInformation)))
      } else {
        None
      }

    RequestDetailForUpdate(
      IDType = "SAFE",
      IDNumber = responseDetail.subscriptionID,
      tradingName = None,
      isGBUser = responseDetail.isGBUser,
      primaryContact = primaryContact,
      secondaryContact = secondaryContact
    )
  }

  def updateSubscription(subscriptionDetails: SubscriptionForDACResponse,
                         userAnswers: UserAnswers): UpdateSubscriptionDetails = {
    UpdateSubscriptionDetails(
      requestCommon = RequestCommonForUpdate.createRequestCommon,
      requestDetail = createRequestDetail(subscriptionDetails.responseDetail, userAnswers)
    )
  }

}

case class UpdateSubscriptionForDACRequest(updateSubscriptionForDACRequest: UpdateSubscriptionDetails)
object UpdateSubscriptionForDACRequest {
  implicit val format: OFormat[UpdateSubscriptionForDACRequest] = Json.format[UpdateSubscriptionForDACRequest]
}
