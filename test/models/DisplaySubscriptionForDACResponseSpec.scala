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
import helpers.JsonFixtures._
import models.subscription._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, Json}

class DisplaySubscriptionForDACResponseSpec extends SpecBase with ScalaCheckPropertyChecks with Generators {

  val responseCommon: ResponseCommon = ResponseCommon(
    status = "OK",
    statusText = None,
    processingDate = "2020-08-09T11:23:45Z",
    returnParameters = None)

  "DisplaySubscriptionForDACResponse" - {
    "must deserialise DisplaySubscriptionForDACResponse" in {

      forAll(validDacID, validOrganisationName, validPersonalName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, organisationName, secondaryName, primaryEmail, secondaryEmail, phoneNumber) =>

          val primaryContact: PrimaryContact = PrimaryContact(Seq(
            ContactInformationForOrganisation(
              organisation = OrganisationDetails(organisationName = organisationName),
              email = primaryEmail, phone = None, mobile = None)
          ))
          val secondaryContact: SecondaryContact = SecondaryContact(Seq(
            ContactInformationForOrganisation(
              organisation = OrganisationDetails(organisationName = secondaryName),
              email = secondaryEmail, phone = Some(phoneNumber), mobile = None)
          ))

          val responseDetail: ResponseDetail = ResponseDetail(
            subscriptionID = safeID,
            tradingName = Some("Trading Name"),
            isGBUser = true,
            primaryContact = primaryContact,
            secondaryContact = Some(secondaryContact))

          val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse = DisplaySubscriptionForDACResponse(
            SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetail))

          val jsonPayload = displaySubscriptionPayload(
            JsString(safeID), JsString(organisationName), JsString(secondaryName), JsString(primaryEmail), JsString(secondaryEmail), JsString(phoneNumber))

          Json.parse(jsonPayload).validate[DisplaySubscriptionForDACResponse].get mustBe displaySubscriptionForDACResponse
      }
    }

    "must serialise DisplaySubscriptionForDACResponse" in {

      forAll(validDacID, validPersonalName, validOrganisationName, validEmailAddress, validEmailAddress, validPhoneNumber) {
        (safeID, name, organisationName, primaryEmail, secondaryEmail, phoneNumber) =>

          val primaryContact: PrimaryContact = PrimaryContact(Seq(
            ContactInformationForIndividual(
              individual = IndividualDetails(firstName = name, lastName = name, middleName = None),
              email = primaryEmail, phone = Some(phoneNumber), mobile = Some(phoneNumber))
          ))
          val secondaryContact: SecondaryContact = SecondaryContact(Seq(
            ContactInformationForOrganisation(
              organisation = OrganisationDetails(organisationName = organisationName),
              email = secondaryEmail, phone = None, mobile = None)
          ))

          val responseDetail: ResponseDetail = ResponseDetail(
            subscriptionID = safeID,
            tradingName = Some("Trading Name"),
            isGBUser = true,
            primaryContact = primaryContact,
            secondaryContact = Some(secondaryContact))

          val displaySubscriptionForDACResponse: DisplaySubscriptionForDACResponse = DisplaySubscriptionForDACResponse(
            SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetail))

          val json = jsonForDisplaySubscription(safeID, name, name, organisationName, primaryEmail, secondaryEmail, phoneNumber)

          Json.toJson(displaySubscriptionForDACResponse) mustBe json
      }
    }
  }

}
