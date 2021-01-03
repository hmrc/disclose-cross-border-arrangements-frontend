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

package models

import base.SpecBase
import generators.Generators
import helpers.JsonFixtures.{updateDetailsJson, updateDetailsJsonNoSecondContact, updateDetailsPayload, updateDetailsPayloadNoSecondContact}
import models.subscription._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, Json}

class UpdateSubscriptionForDACRequestSpec extends SpecBase with Generators with ScalaCheckPropertyChecks {

  val requestParameter = Seq(RequestParameter("Name", "Value"))

  val requestCommon: RequestCommonForUpdate = RequestCommonForUpdate(
    regime = "DAC",
    receiptDate = "2020-09-23T16:12:11Z",
    acknowledgementReference = "AB123c",
    originatingSystem = "MDTP",
    requestParameters = Some(requestParameter)
  )

  "UpdateSubscriptionForDACRequest" - {

    "must deserialise UpdateSubscriptionForDACRequest" in {
      forAll(validPersonalName, validPersonalName, validEmailAddress, validOrganisationName, validEmailAddress, validPhoneNumber) {
        (firstName, lastName, primaryEmail, organisationName, secondaryEmail, phone) =>

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val secondaryContact = SecondaryContact(
            Seq(ContactInformationForOrganisation(OrganisationDetails(organisationName), secondaryEmail, Some(phone), None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = "IDNumber",
            tradingName = None,
            isGBUser =  false,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayload(JsString(firstName), JsString(lastName), JsString(primaryEmail),
            JsString(organisationName), JsString(secondaryEmail), JsString(phone))

          Json.parse(jsonPayload).validate[UpdateSubscriptionForDACRequest].get mustBe updateRequest
      }
    }

    "must deserialise UpdateSubscriptionForDACRequest without a second contact" in {
      forAll(validPersonalName, validPersonalName, validEmailAddress) {
        (firstName, lastName, primaryEmail) =>

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = "IDNumber",
            tradingName = None,
            isGBUser =  true,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayloadNoSecondContact(JsString(firstName), JsString(lastName), JsString(primaryEmail))

          Json.parse(jsonPayload).validate[UpdateSubscriptionForDACRequest].get mustBe updateRequest
      }
    }

    "must serialise UpdateSubscriptionForDACRequest" in {
      forAll(validPersonalName, validPersonalName, validEmailAddress, validOrganisationName, validEmailAddress, validPhoneNumber) {
        (firstName, lastName, primaryEmail, organisationName, secondaryEmail, phone) =>
          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val secondaryContact = SecondaryContact(
            Seq(ContactInformationForOrganisation(OrganisationDetails(organisationName), secondaryEmail, Some(phone), None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = "IDNumber",
            tradingName = None,
            isGBUser =  false,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJson(firstName, lastName, primaryEmail,
            organisationName, secondaryEmail, phone)
      }
    }

    "must serialise UpdateSubscriptionForDACRequest without secondary contact" in {
      forAll(validPersonalName, validPersonalName, validEmailAddress) {
        (firstName, lastName, primaryEmail) =>
          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = "IDNumber",
            tradingName = None,
            isGBUser =  true,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJsonNoSecondContact(firstName, lastName, primaryEmail)
      }
    }

  }

}
