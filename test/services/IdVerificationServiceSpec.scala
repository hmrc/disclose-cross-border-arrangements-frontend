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

package services

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import models.{Dac6MetaData, GenericError, SubmissionDetails, SubmissionHistory, ValidationFailure, ValidationSuccess}
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.time.Seconds
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class IdVerificationServiceSpec extends SpecBase{

  val arrangementId1 = "GBA20200101AAA123"
  val arrangementId2 = "GBA20200101BBB456"

  val disclosureId1 = "GBD20200101AAA123"
  val disclosureId2 = "GBD20200101BBB456"

  val mockConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

  val idVerificationService = new IdVerificationService(mockConnector)

  val downloadSource = "download-src"

  implicit val hc = HeaderCarrier()
  import scala.concurrent.ExecutionContext.Implicits._

  val testXml =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <ArrangementID>AAA000000000</ArrangementID>
      <DisclosureID>AAA000000000</DisclosureID>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
        <RelevantTaxPayers>
          <RelevantTaxpayer>
          </RelevantTaxpayer>
          <RelevantTaxpayer>
            <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
          </RelevantTaxpayer>
        </RelevantTaxPayers>
        <DisclosureInformation>
          <ImplementingDate>2020-01-14</ImplementingDate>
        </DisclosureInformation>
        <DisclosureInformation>
          <ImplementingDate>2018-06-25</ImplementingDate>
        </DisclosureInformation>
      </DAC6Disclosures>
    </DAC6_Arrangement>


  "IdVerificationService" -{
    "verifyIds" -{
      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6ADD" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                  doAllRelevantTaxpayersHaveImplementingDate = true))

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(SubmissionHistory(List())))
        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)

      }

      "should return a ValidationFailure if ArrangementId matches hmrcs record for DAC6ADD" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                             doAllRelevantTaxpayersHaveImplementingDate = true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(SubmissionHistory(List())))

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(false))
        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(6, "ArrangementID does not match HMRC's records")))

      }

      "should return a ValidationSuccess if disclosureId relates to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)

      }

      "should return a ValidationFailure if disclosureId does not relate to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId2),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(7, "DisclosureID has not been generated by this individual or organisation")))

      }

      "should return a ValidationFailure if disclosureId does not relate to the given ArrangementId" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails1 = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionDetails2 = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId2),
                                                  disclosureID = Some(disclosureId2),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails1, submissionDetails2))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(7, "DisclosureID does not match the ArrangementID provided")))

      }

      "should return a ValidationSuccess if DAC6ADD for a marketable arrangment has implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails1 = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = true)


        val submissionHistory = SubmissionHistory(List(submissionDetails1))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
      }

      "should return a ValidationFailure if DAC6ADD for a marketable arrangement does not have implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), doAllRelevantTaxpayersHaveImplementingDate = false))

        val submissionDetails1 = SubmissionDetails(enrolmentID = "enrolmentID",
                                                  submissionTime = DateTime.now(),
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = true)


        val submissionHistory = SubmissionHistory(List(submissionDetails1))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(10, "taxpayerDate Error")))
      }


//      "DisclosureID does not match HMRC's records
//
//      DisclosureID has not been generated by this individual or organisation
//
//      DisclosureID does not match the ArrangementID provided"

    }


  }


}
