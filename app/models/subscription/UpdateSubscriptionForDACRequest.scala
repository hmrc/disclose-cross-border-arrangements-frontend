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

package models.subscription

import models.UserAnswers
import pages.contactdetails._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

case class RequestCommonForUpdate(regime: String,
                                  receiptDate: String,
                                  acknowledgementReference: String,
                                  originatingSystem: String,
                                  requestParameters: Option[Seq[RequestParameter]]
)

object RequestCommonForUpdate {
  implicit val format: OFormat[RequestCommonForUpdate] = Json.format[RequestCommonForUpdate]

  def createRequestCommon: RequestCommonForUpdate = {
    //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    //Generate a 32 chars UUID without hyphens
    val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")

    RequestCommonForUpdate(
      regime = "DAC",
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = acknowledgementReference,
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
                                  secondaryContact: Option[SecondaryContact]
)

object RequestDetailForUpdate {

  implicit val reads: Reads[RequestDetailForUpdate] = (
    (__ \ "IDType").read[String] and
      (__ \ "IDNumber").read[String] and
      (__ \ "tradingName").readNullable[String] and
      (__ \ "isGBUser").read[Boolean] and
      (__ \ "primaryContact").read[Seq[PrimaryContact]] and
      (__ \ "secondaryContact").readNullable[Seq[SecondaryContact]]
  )(
    (idt, idr, tn, gb, pc, sc) => RequestDetailForUpdate(idt, idr, tn, gb, pc.head, sc.map(_.head))
  )

  implicit lazy val writes: Writes[RequestDetailForUpdate] = (
    (__ \ "IDType").write[String] and
      (__ \ "IDNumber").write[String] and
      (__ \ "tradingName").writeNullable[String] and
      (__ \ "isGBUser").write[Boolean] and
      (__ \ "primaryContact").write[Seq[PrimaryContact]] and
      (__ \ "secondaryContact").writeNullable[Seq[SecondaryContact]]
  )(
    r => (r.IDType, r.IDNumber, r.tradingName, r.isGBUser, Seq(r.primaryContact), r.secondaryContact.map(Seq(_)))
  )
}

case class UpdateSubscriptionDetails(requestCommon: RequestCommonForUpdate, requestDetail: RequestDetailForUpdate)

object UpdateSubscriptionDetails {

  implicit val format: OFormat[UpdateSubscriptionDetails] = Json.format[UpdateSubscriptionDetails]

  private def buildContactInformation(contactInformation: Seq[ContactInformation], userAnswers: UserAnswers): ContactInformation =
    //Note: There should only be one item in contact information
    contactInformation.head match {
      case ContactInformationForIndividual(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(ContactEmailAddressPage) match {
          case Some(email) => email
          case None        => email
        }

        val telephone =
          (userAnswers.get(ContactTelephoneNumberPage), userAnswers.get(HaveContactPhonePage)) match {
            case (_, Some(false))    => None
            case (Some(newPhone), _) => Some(newPhone)
            case _                   => phone
          }

        ContactInformationForIndividual(details, emailAddress, telephone, mobile)
      case ContactInformationForOrganisation(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(ContactEmailAddressPage) match {
          case Some(email) => email
          case None        => email
        }

        val telephone =
          (userAnswers.get(ContactTelephoneNumberPage), userAnswers.get(HaveContactPhonePage)) match {
            case (_, Some(false))    => None
            case (Some(newPhone), _) => Some(newPhone)
            case _                   => phone
          }

        val organisationDetails = userAnswers.get(ContactNamePage) match {
          case Some(name) => OrganisationDetails(name)
          case None       => details
        }

        ContactInformationForOrganisation(organisationDetails, emailAddress, telephone, mobile)
    }

  private def buildSecondaryContactInformation(contactInformation: Seq[ContactInformation], userAnswers: UserAnswers): ContactInformationForOrganisation =
    //Note: Secondary contact name is only one field in the registration journey. It's always ContactInformationForOrganisation
    contactInformation.head match {
      case ContactInformationForOrganisation(details, email, phone, mobile) =>
        val emailAddress = userAnswers.get(SecondaryContactEmailAddressPage) match {
          case Some(email) => email
          case None        => email
        }

        val telephone =
          (userAnswers.get(SecondaryContactTelephoneNumberPage), userAnswers.get(HaveSecondaryContactPhonePage)) match {
            case (_, Some(false))    => None
            case (Some(newPhone), _) => Some(newPhone)
            case _                   => phone
          }

        val organisationDetails = userAnswers.get(SecondaryContactNamePage) match {
          case Some(name) => OrganisationDetails(name)
          case None       => details
        }

        ContactInformationForOrganisation(organisationDetails, emailAddress, telephone, mobile)
      case _ => throw new Exception("Unable to build secondary contact")
    }

  private def createRequestDetail(responseDetail: ResponseDetail, userAnswers: UserAnswers): RequestDetailForUpdate = {
    val primaryContact =
      buildContactInformation(responseDetail.primaryContact.contactInformation, userAnswers) match {
        case contactInformation @ ContactInformationForIndividual(_, _, _, _)   => PrimaryContact(Seq(contactInformation))
        case contactInformation @ ContactInformationForOrganisation(_, _, _, _) => PrimaryContact(Seq(contactInformation))
      }

    val secondaryContact: Option[SecondaryContact] =
      responseDetail.secondaryContact.fold(Option.empty[SecondaryContact]) {
        secondaryContact =>
          userAnswers.get(HaveSecondContactPage) match {
            case Some(false) => None
            case _ =>
              val contactInformation = buildSecondaryContactInformation(secondaryContact.contactInformation, userAnswers)
              Some(SecondaryContact(Seq(contactInformation)))
          }
      }

    RequestDetailForUpdate(
      IDType = "DAC",
      IDNumber = responseDetail.subscriptionID,
      tradingName = None,
      isGBUser = responseDetail.isGBUser,
      primaryContact = primaryContact,
      secondaryContact = secondaryContact
    )
  }

  def updateSubscription(subscriptionDetails: SubscriptionForDACResponse, userAnswers: UserAnswers): UpdateSubscriptionDetails =
    UpdateSubscriptionDetails(
      requestCommon = RequestCommonForUpdate.createRequestCommon,
      requestDetail = createRequestDetail(subscriptionDetails.responseDetail, userAnswers)
    )

}

case class UpdateSubscriptionForDACRequest(updateSubscriptionForDACRequest: UpdateSubscriptionDetails)

object UpdateSubscriptionForDACRequest {
  implicit val format: OFormat[UpdateSubscriptionForDACRequest] = Json.format[UpdateSubscriptionForDACRequest]
}
