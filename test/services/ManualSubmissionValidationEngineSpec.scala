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

package services

import base.SpecBase
import cats.data.ReaderT
import cats.implicits._
import connectors.CrossBorderArrangementsConnector
import helpers.{BusinessRulesErrorMessageHelper, XmlErrorMessageHelper}
import models.{Dac6MetaData, GenericError, SaxParseError, Validation, ValidationFailure, ValidationSuccess}
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, _}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.xml.{Elem, NodeSeq}

class ManualSubmissionValidationEngineSpec  extends SpecBase with MockitoSugar {

  val xsdError = "xsd-error"
  val defaultError = "There is a problem with this line number"
  val lineNumber = 0
  val noErrors: ListBuffer[SaxParseError] = ListBuffer()

  val addressError1 = SaxParseError(20, "cvc-minLength-valid: Value '' with length = '0' is " +
    "not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")

  val addressError2 = SaxParseError(20, "cvc-type.3.1.3: The value '' of element 'Street' is not valid.")

  val over400 = "a" * 401
  val over4000 = "a" * 4001

  val maxLengthError1 = SaxParseError(116, s"cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'.")
  val maxlengthError2 = SaxParseError(116, s"cvc-type.3.1.3: The value '$over400' of element 'BuildingIdentifier' is not valid.")

  val maxLengthError3 = SaxParseError(116, s"cvc-maxLength-valid: Value '$over4000' with length = '4001' is not facet-valid with respect to maxLength '4000' for type 'StringMin1Max4000_Type'.")
  val maxlengthError4 = SaxParseError(116, s"cvc-type.3.1.3: The value '$over4000' of element 'NationalProvision' is not valid.")

  val countryCodeError1 = SaxParseError(123,"cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration.")
  val countryCodeError2 = SaxParseError(123, "cvc-type.3.1.3: The value 'Raneevev' of element 'Country' is not valid.")

  val concernedMsError1 = SaxParseError(177,"cvc-enumeration-valid: Value 'CdvvdvZ' is not facet-valid with respect to enumeration '[AT, SE, GB]'. It must be a value from the enumeration.")
  val concernedMsError2 = SaxParseError(177,"cvc-type.3.1.3: The value 'CdvvdvZ' of element 'ConcernedMS' is not valid.")


  val countryExemptionError1 = SaxParseError(133,"cvc-enumeration-valid: Value 'eevev' is not facet-valid with respect to enumeration '[AF, VE, VN, VG, VI, WF, EH, YE, ZM, ZW, XK, XX]'. It must be a value from the enumeration.")
  val countryExemptionError2 = SaxParseError(133,"cvc-type.3.1.3: The value 'eevev' of element 'CountryExemption' is not valid.")

  val reasonError1 = SaxParseError(169,"cvc-enumeration-valid: Value 'DAC670vdvdvd4' is not facet-valid with respect to enumeration '[DAC6701, DAC6702, DAC6703, DAC6704]'. It must be a value from the enumeration.")
  val reasonError2 = SaxParseError(169,"cvc-type.3.1.3: The value 'DAC670vdvdvd4' of element 'Reason' is not valid.")

  val intermediaryCapacityError1 = SaxParseError(129,"cvc-enumeration-valid: Value 'DAC61102fefef' is not facet-valid with respect to enumeration '[DAC61101, DAC61102]'. It must be a value from the enumeration.")
  val intermediaryCapacityError2 = SaxParseError(129,"cvc-type.3.1.3: The value 'DAC61102fefef' of element 'Capacity' is not valid.")

  val relevantTpDiscloserCapacityError1 = SaxParseError(37,"cvc-enumeration-valid: Value 'DAC61105hhh' is not facet-valid with respect to enumeration '[DAC61104, DAC61105, DAC61106]'. It must be a value from the enumeration.")
  val relevantTpDiscloserCapacityError2 = SaxParseError(37,"cvc-type.3.1.3: The value 'DAC61105hhh' of element 'Capacity' is not valid.")

  val missingAddressErrors = ListBuffer(addressError1, addressError2)

  val cityError1 = SaxParseError(27, "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")
  val cityError2 = SaxParseError(27, "cvc-type.3.1.3: The value '' of element 'City' is not valid.")
  val missingCityErrors = ListBuffer(cityError1, cityError2)

  val invalidAttributeCodeError = SaxParseError(175,"cvc-attribute.3: The value 'VUVs' of attribute 'currCode' on element 'Amount' is not valid with respect to its type, 'currCode_Type'.")

  val issuedByError1 = SaxParseError(18,"cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration.")
  val issuedByError2 = SaxParseError(18,"cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'TIN' is not valid with respect to its type, 'CountryCode_Type'.")

val enrolmentId = "123456"

  trait SetUp {
    val doesFileHaveBusinessErrors = false

    val mockXmlValidationService: XMLValidationService = mock[XMLValidationService]
    val mockCrossBorderArrangementsConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

    val mockMetaDataValidationService: MetaDataValidationService = mock[MetaDataValidationService]

    val mockAuditService: AuditService = mock[AuditService]

    val businessRulesErrorMessageHelper: BusinessRulesErrorMessageHelper = new BusinessRulesErrorMessageHelper

    val xmlErrorMessageHelper: XmlErrorMessageHelper = new XmlErrorMessageHelper

    val mockBusinessRuleValidationService: BusinessRuleValidationService =
      new BusinessRuleValidationService(mockCrossBorderArrangementsConnector) {

        val dummyReader: ReaderT[Option, NodeSeq, Boolean] =
          ReaderT[Option, NodeSeq, Boolean](xml => {
            Some(!doesFileHaveBusinessErrors)
          })

        def dummyValidation(): ReaderT[Option, NodeSeq, Validation] = {
          for {
            result <- dummyReader
          } yield
            Validation(
              key = defaultError,
              value = result
            )
      }

      override def validateFile()(implicit hc: HeaderCarrier, ec: ExecutionContext): ReaderT[Option, NodeSeq, Future[Seq[Validation]]] = {
        for {
          v1 <- dummyValidation()
        } yield
          Future.successful(Seq(v1).filterNot(_.value))
      }

      override def extractDac6MetaData(): ReaderT[Option, NodeSeq, Dac6MetaData] = {
        for {
          _ <-  dummyReader
        }yield {
          Dac6MetaData("DAC6NEW", disclosureInformationPresent = true,
                       initialDisclosureMA = false, messageRefId = "messageRefId")

        }
      }

    }

    val validationEngine = new ManualSubmissionValidationEngine(mockXmlValidationService,
                                                mockBusinessRuleValidationService,
                                                xmlErrorMessageHelper,
                                                businessRulesErrorMessageHelper,
                                                mockMetaDataValidationService,
                                                mockAuditService)

    val source = "src"
    val elem: Elem = <dummyElement>Test</dummyElement>
    val mockXML: Elem = <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
    val mockMetaData = Some(Dac6MetaData("DAC6NEW", disclosureInformationPresent = true,
                                        initialDisclosureMA = false, messageRefId = "messageRefId"))

  }
    "ValidateManualSubmission" - {
      "must return no errors when xml with no business errors received" in new SetUp {

        when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
        when(mockMetaDataValidationService.verifyMetaData(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))

        val xml = <dummyTag></dummyTag>

        Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe Some(Seq())

        verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any())(any())

      }

      "must return errors when xml with businessErrors received" in new SetUp {

        override val doesFileHaveBusinessErrors = true

        when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
        when(mockMetaDataValidationService.verifyMetaData(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))

        val xml = <dummyTag></dummyTag>

        Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe Some(Seq(defaultError))
        verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any())(any())

      }

      "must return errors when xml with metaData errors received" in new SetUp {

        when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)

        when(mockMetaDataValidationService.verifyMetaData(any(), any())(any(), any())).thenReturn(
          Future.successful(Seq(Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false))))

        val xml = <dummyTag></dummyTag>

        Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe Some(Seq("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"))
        verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any())(any())

      }

      "must return none when xml parsing fails and audit failure" in new SetUp {

        when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(addressError1))
        when(mockMetaDataValidationService.verifyMetaData(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))

        val xml = <dummyTag></dummyTag>

        Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe None

        verify(mockAuditService, times(1)).auditManualSubmissionParseFailure(any(), any())(any())


      }


    }
 }
