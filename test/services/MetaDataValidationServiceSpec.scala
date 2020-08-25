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

import java.time.LocalDateTime

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import models.{Dac6MetaData, GenericError, SubmissionDetails, SubmissionHistory, ValidationFailure, ValidationSuccess}
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.MockitoAnnotations.Mock
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.time.Seconds
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class MetaDataValidationServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach {
    Mockito.reset(mockConnector)
  }

  val arrangementId1 = "GBA20200101AAA123"
  val arrangementId2 = "GBA20200101BBB456"

  val disclosureId1 = "GBD20200101AAA123"
  val disclosureId2 = "GBD20200101BBB456"

  val mockConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

  val idVerificationService = new MetaDataValidationService(mockConnector)

  val downloadSource = "download-src"

  implicit val hc = HeaderCarrier()

  implicit val postFixOps = scala.language.postfixOps
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val enrolmentId = "123456"

  val submissionDateTime1 = LocalDateTime.now()
  val submissionDateTime2 = submissionDateTime1.plusDays(1)
  val submissionDateTime3 = submissionDateTime1.plusDays(2)

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
      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6ADD if they are filing under another users arrangemntid" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                  doAllRelevantTaxpayersHaveImplementingDate = true))

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(SubmissionHistory(List())))
        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
        verify(mockConnector, times(1)).verifyArrangementId(any())(any())

      }

      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6ADD if they are filing under their own arrangemntid" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                  doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails))

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))
        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
        verify(mockConnector, times(0)).verifyArrangementId(any())(any())


      }

      "should return a ValidationFailure if ArrangementId matches hmrcs record for DAC6ADD" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                             doAllRelevantTaxpayersHaveImplementingDate = true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(SubmissionHistory(List())))

        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(false))
        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(6, "ArrangementID does not match HMRC's records")))
        verify(mockConnector, times(1)).verifyArrangementId(any())(any())
      }

      "should return a ValidationSuccess if disclosureId relates to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)

      }

      "should return a ValidationFailure if disclosureId does not relate to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId2),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(7, "DisclosureID has not been generated by this individual or organisation")))

      }

      "should return a ValidationFailure if disclosureId does not relate to the given ArrangementId" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime2,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId2),
                                                  disclosureID = Some(disclosureId2),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = false)

        val submissionHistory = SubmissionHistory(List(submissionDetails1, submissionDetails2))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(7, "DisclosureID does not match the ArrangementID provided")))

      }

      "should return a ValidationSuccess if DAC6ADD for a marketable arrangment has implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), doAllRelevantTaxpayersHaveImplementingDate = true))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6REP",
                                                  initialDisclosureMA = true)


        val submissionHistory = SubmissionHistory(List(submissionDetails1))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
      }

      "should return a ValidationFailure if DAC6ADD for a marketable arrangement does not have implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
                                                 doAllRelevantTaxpayersHaveImplementingDate = false))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6NEW",
                                                  initialDisclosureMA = true)


        val submissionHistory = SubmissionHistory(List(submissionDetails1))
        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(10, "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate")))
      }

      "should return a ValidationFailure if DAC6REP for a marketable arrangement does not have implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), doAllRelevantTaxpayersHaveImplementingDate = false))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
                                                  submissionTime = submissionDateTime1,
                                                  fileName = "fileName.xml",
                                                  arrangementID = Some(arrangementId1),
                                                  disclosureID = Some(disclosureId1),
                                                  importInstruction = "DAC6NEW",
                                                  initialDisclosureMA = true)


        val submissionHistory = SubmissionHistory(List(submissionDetails1))
        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(10, "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate")))
      }

      "should return a ValidationSucces if DAC6ADD for an arrangment which is no longer a marketable arrangement and does not have implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          doAllRelevantTaxpayersHaveImplementingDate = false))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6NEW",
          initialDisclosureMA = true)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime2,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false)


        val submissionHistory = SubmissionHistory(List(submissionDetails1, submissionDetails2))
        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
      }

      "should return a ValidationSucces if DAC6ADD for an arrangment which is no longer a marketable arrangement and does not have implementingDates " +
      "populated for new RelevantTaxpayers 2" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          doAllRelevantTaxpayersHaveImplementingDate = false))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6NEW",
          initialDisclosureMA = true)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime2,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = true)

        val submissionDetails3 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime3,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false)


        val submissionHistory = SubmissionHistory(List(submissionDetails1, submissionDetails2, submissionDetails3))
        when(mockConnector.verifyArrangementId(any())(any())).thenReturn(Future.successful(true))
        when(mockConnector.getSubmissionHistory(any())(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, dac6MetaData, enrolmentId), 10 seconds)
        result mustBe ValidationSuccess(downloadSource, dac6MetaData)
      }


      "should return a ValidationFailure if metData notDefined" in {

        val result = Await.result(idVerificationService.verifyMetaData(downloadSource, testXml, None, enrolmentId), 10 seconds)
        result mustBe ValidationFailure(List(GenericError(0, "File does not contain necessary data")))
      }

    }


  }


}
