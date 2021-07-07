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
import models.subscription.{SubscriptionForDACResponse, _}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.contactdetails._
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

  val individualPrimaryContact: PrimaryContact = PrimaryContact(
    Seq(
      ContactInformationForIndividual(individual = IndividualDetails(firstName = "FirstName", lastName = "LastName", middleName = None),
                                      email = "email@email.com",
                                      phone = Some("07111222333"),
                                      mobile = None
      )
    )
  )

  val primaryContact: PrimaryContact = PrimaryContact(
    Seq(
      ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Organisation Name"),
                                        email = "email@email.com",
                                        phone = None,
                                        mobile = None
      )
    )
  )

  val secondaryContact: SecondaryContact = SecondaryContact(
    Seq(
      ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Secondary contact name"),
                                        email = "email2@email.com",
                                        phone = Some("07111222333"),
                                        mobile = None
      )
    )
  )

  val responseCommon: ResponseCommon = ResponseCommon(status = "OK", statusText = None, processingDate = "2021-03-10T12:02:53Z", returnParameters = None)

  val responseDetail: ResponseDetail = ResponseDetail(subscriptionID = "XE0001234567890",
                                                      tradingName = Some("Trading Name"),
                                                      isGBUser = true,
                                                      primaryContact = primaryContact,
                                                      secondaryContact = Some(secondaryContact)
  )

  val subscriptionForDACResponse: SubscriptionForDACResponse =
    SubscriptionForDACResponse(responseCommon = responseCommon, responseDetail = responseDetail)

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
            isGBUser = false,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayload(JsString(firstName),
                                                 JsString(lastName),
                                                 JsString(primaryEmail),
                                                 JsString(organisationName),
                                                 JsString(secondaryEmail),
                                                 JsString(phone)
          )

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
            isGBUser = true,
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
            isGBUser = false,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJson(firstName, lastName, primaryEmail, organisationName, secondaryEmail, phone)
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
            isGBUser = true,
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

    "updateSubscription with contact detail changes" - {

      "must build RequestDetailForUpdate with new primary phone and email if an Individual user has updated it" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ContactEmailAddressPage, "email2@email.com")
          .success
          .value
          .set(HaveContactPhonePage, true)
          .success
          .value
          .set(ContactTelephoneNumberPage, "07111111111")
          .success
          .value

        val subscriptionForDACResponse: SubscriptionForDACResponse =
          SubscriptionForDACResponse(responseCommon = responseCommon,
                                     responseDetail = responseDetail.copy(primaryContact = individualPrimaryContact, secondaryContact = None)
          )

        val expectedPrimaryContact: PrimaryContact = PrimaryContact(
          Seq(
            ContactInformationForIndividual(
              individual = IndividualDetails(firstName = "FirstName", lastName = "LastName", middleName = None),
              email = "email2@email.com",
              phone = Some("07111111111"),
              mobile = None
            )
          )
        )

        val expected = RequestDetailForUpdate(
          IDType = "DAC",
          IDNumber = "XE0001234567890",
          tradingName = None,
          isGBUser = true,
          primaryContact = expectedPrimaryContact,
          secondaryContact = None
        )

        val result = UpdateSubscriptionDetails.updateSubscription(subscriptionForDACResponse, userAnswers)

        result.requestDetail mustBe expected
      }

      "must build RequestDetailForUpdate with new primary phone if user has updated it" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(HaveContactPhonePage, true)
          .success
          .value
          .set(ContactTelephoneNumberPage, "07111222333")
          .success
          .value

        val primaryContact = PrimaryContact(
          Seq(
            ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Organisation Name"),
                                              email = "email@email.com",
                                              phone = Some("07111222333"),
                                              mobile = None
            )
          )
        )

        val expected = RequestDetailForUpdate(
          IDType = "DAC",
          IDNumber = "XE0001234567890",
          tradingName = None,
          isGBUser = true,
          primaryContact = primaryContact,
          secondaryContact = Some(secondaryContact)
        )

        val result = UpdateSubscriptionDetails.updateSubscription(subscriptionForDACResponse, userAnswers)

        result.requestDetail mustBe expected
      }

      "must build RequestDetailForUpdate with updated secondary contact details e.g. New name and no phone" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(HaveSecondContactPage, true)
          .success
          .value
          .set(SecondaryContactNamePage, "New secondary contact name")
          .success
          .value
          .set(HaveSecondaryContactPhonePage, false)
          .success
          .value

        val secondaryContact = SecondaryContact(
          Seq(
            ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "New secondary contact name"),
                                              email = "email2@email.com",
                                              phone = None,
                                              mobile = None
            )
          )
        )

        val expected = RequestDetailForUpdate(
          IDType = "DAC",
          IDNumber = "XE0001234567890",
          tradingName = None,
          isGBUser = true,
          primaryContact = primaryContact,
          secondaryContact = Some(secondaryContact)
        )

        val result = UpdateSubscriptionDetails.updateSubscription(subscriptionForDACResponse, userAnswers)

        result.requestDetail mustBe expected
      }

      "must build RequestDetailForUpdate with new secondary phone if user has updated it" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(HaveSecondaryContactPhonePage, true)
          .success
          .value
          .set(SecondaryContactTelephoneNumberPage, "07111111111")
          .success
          .value

        val secondaryContact = SecondaryContact(
          Seq(
            ContactInformationForOrganisation(organisation = OrganisationDetails(organisationName = "Secondary contact name"),
                                              email = "email2@email.com",
                                              phone = Some("07111111111"),
                                              mobile = None
            )
          )
        )

        val expected = RequestDetailForUpdate(
          IDType = "DAC",
          IDNumber = "XE0001234567890",
          tradingName = None,
          isGBUser = true,
          primaryContact = primaryContact,
          secondaryContact = Some(secondaryContact)
        )

        val result = UpdateSubscriptionDetails.updateSubscription(subscriptionForDACResponse, userAnswers)

        result.requestDetail mustBe expected
      }

      "must build RequestDetailForUpdate without secondary contact if user has removed them" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(HaveSecondContactPage, false)
          .success
          .value

        val expected = RequestDetailForUpdate(
          IDType = "DAC",
          IDNumber = "XE0001234567890",
          tradingName = None,
          isGBUser = true,
          primaryContact = primaryContact,
          secondaryContact = None
        )

        val result = UpdateSubscriptionDetails.updateSubscription(subscriptionForDACResponse, userAnswers)

        result.requestDetail mustBe expected
      }
    }

  }

}
