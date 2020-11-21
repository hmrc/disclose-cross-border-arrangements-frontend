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
import java.util.{Calendar, GregorianCalendar}

import base.SpecBase
import connectors.CrossBorderArrangementsConnector
import fixtures.XMLFixture
import models.{Dac6MetaData, SubmissionDetails, Validation}
import org.mockito.Mockito.{when, _}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRuleValidationServiceSpec extends SpecBase with MockitoSugar with IntegrationPatience {

  val mockCrossBorderArrangementsConnector: CrossBorderArrangementsConnector = mock[CrossBorderArrangementsConnector]

  override def beforeEach: Unit = {
    reset(mockCrossBorderArrangementsConnector)
  }

  val application: Application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[CrossBorderArrangementsConnector].toInstance(mockCrossBorderArrangementsConnector)
    ).build()

  "BusinessRuleValidationService" - {
    "must be able to extract the initial disclosure when set" in {
      val xml = XMLFixture.dac6NotInitialDisclosureMA

      BusinessRuleValidationService.isInitialDisclosureMA(xml).value mustBe false
    }

    "must be able to use default initial disclosure when not set" in {
      val xml = XMLFixture.dac6InitialDisclosureMANotSet

      BusinessRuleValidationService.isInitialDisclosureMA(xml).value mustBe false
    }

    "must be able to extract relevant taxpayers" in {
      val xml = XMLFixture.dac6RelevantTaxPayers

      BusinessRuleValidationService.noOfRelevantTaxPayers(xml).value mustBe 2
    }

    "must be able to find no relevant taxpayers" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.noOfRelevantTaxPayers(xml).value mustBe 0
    }

    "must fail validation if RelevantTaxPayer date of births are on or after 01/01/1903" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1902-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if disclosing date of births are on or after 01/01/1903" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <Disclosing>
            <ID>
              <Individual>
                <IndividualName>
                  <FirstName>John</FirstName>
                  <LastName>Charles</LastName>
                  <Suffix>Mr</Suffix>
                </IndividualName>
                <BirthDate>1902-12-31</BirthDate>
                <BirthPlace>Random Town</BirthPlace>
                <TIN issuedBy="GB">AA000000D</TIN>
                <Address>
                  <Street>Random Street</Street>
                  <BuildingIdentifier>No 10</BuildingIdentifier>
                  <SuiteIdentifier>Random Suite</SuiteIdentifier>
                  <FloorIdentifier>Second</FloorIdentifier>
                  <DistrictName>Random District</DistrictName>
                  <POB>48</POB>
                  <PostCode>SW1A 4GG</PostCode>
                  <City>Random City</City>
                  <Country>GB</Country>
                </Address>
                <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                <ResCountryCode>VU</ResCountryCode>
              </Individual>
            </ID>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.DisclosingBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if intermediary date of births are on or after 01/01/1903" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
           <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
             <Intermediaries>
               <Intermediary>
                 <ID>
                   <Individual>
                     <IndividualName>
                       <FirstName>Larry</FirstName>
                       <LastName>C</LastName>
                       <Suffix>DDD</Suffix>
                     </IndividualName>
                     <BirthDate>1902-12-31</BirthDate>
                     <BirthPlace>BirthPlace</BirthPlace>
                     <TIN issuedBy="GB">AA000000D</TIN>
                     <Address>
                       <Street>Downing Street</Street>
                       <BuildingIdentifier>No 10</BuildingIdentifier>
                       <SuiteIdentifier>Suite</SuiteIdentifier>
                       <FloorIdentifier>Second</FloorIdentifier>
                       <DistrictName>DistrictName</DistrictName>
                       <POB>48</POB>
                       <PostCode>SW1A 4GG</PostCode>
                       <City>London</City>
                       <Country>GB</Country>
                     </Address>
                     <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                     <ResCountryCode>VU</ResCountryCode>
                   </Individual>
                 </ID>
                 <Capacity>DAC61102</Capacity>
                 <NationalExemption>
                   <Exemption>true</Exemption>
                   <CountryExemptions>
                     <CountryExemption>VU</CountryExemption>
                   </CountryExemptions>
                 </NationalExemption>
               </Intermediary>
             </Intermediaries>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if affected persons date of births are on or after 01/01/1903" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
           <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
             <AffectedPersons>
               <AffectedPerson>
                 <AffectedPersonID>
                   <Individual>
                     <IndividualName>
                       <FirstName>FirstName</FirstName>
                       <LastName>LastName</LastName>
                       <Suffix>Suffix</Suffix>
                     </IndividualName>
                     <BirthDate>1902-12-31</BirthDate>
                     <BirthPlace>BirthPlace</BirthPlace>
                     <TIN issuedBy="GB">AB000000D</TIN>
                     <Address>
                       <Street>Street</Street>
                       <BuildingIdentifier>No 10</BuildingIdentifier>
                       <SuiteIdentifier>BuildingIdentifier</SuiteIdentifier>
                       <FloorIdentifier>Second</FloorIdentifier>
                       <DistrictName>DistrictName</DistrictName>
                       <POB>48</POB>
                       <PostCode>SW1A 4GG</PostCode>
                       <City>City</City>
                       <Country>GB</Country>
                     </Address>
                     <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                     <ResCountryCode>VU</ResCountryCode>
                   </Individual>
                 </AffectedPersonID>
               </AffectedPerson>
             </AffectedPersons>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must pass validation if date of births are on a after 01/01/1903" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1903-01-01</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List()
      }
    }

    "must pass validation if an initial disclosure marketable arrangement has one or more relevant taxpayers" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>true</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      service.validateInitialDisclosureHasRelevantTaxPayer()(xml).get.value mustBe true
    }

    "must not pass validation if an initial disclosure marketable arrangement does not have one or more relevant taxpayers" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>true</InitialDisclosureMA>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      service.validateInitialDisclosureHasRelevantTaxPayer()(xml).get.value mustBe false
    }

    "must correctly report presence of RelevantTaxpayerDiscloser" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <RelevantTaxpayerDiscloser>
                  <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                  <Capacity>DAC61105</Capacity>
                </RelevantTaxpayerDiscloser>
              </Liability>
              </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.hasRelevantTaxpayerDiscloser(xml).value mustBe true
    }

    "must correctly report absence of RelevantTaxpayerDiscloser" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <IntermediaryDiscloser>
                  <IntermediaryNexus>INEXb</IntermediaryNexus>
                  <Capacity>DAC61101</Capacity>
                </IntermediaryDiscloser>
              </Liability>
            </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.hasRelevantTaxpayerDiscloser(xml).value mustBe false
    }
  }

  "must correctly validate a RelevantTaxPayer with a RelevantTaxpayerDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()(xml).get.value mustBe true
  }

  "must correctly fail validation for a RelevantTaxpayerDiscloser without a RelevantTaxPayer" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()(xml).get.value mustBe false
  }

  "must correctly report presence of IntermediaryDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasIntermediaryDiscloser(xml).value mustBe true
  }

  "must correctly report absence of IntermediaryDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasIntermediaryDiscloser(xml).value mustBe false
  }

  "must correctly count the number of intermediaries" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <Intermediaries>
              <Intermediary></Intermediary>
            </Intermediaries>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.noOfIntermediaries(xml).value mustBe 1
  }

  "must correctly report absence of Intermediary" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <IntermediaryDiscloser>
                  <IntermediaryNexus>INEXb</IntermediaryNexus>
                  <Capacity>DAC61101</Capacity>
                </IntermediaryDiscloser>
              </Liability>
              <RelevantTaxPayers>
                <RelevantTaxpayer></RelevantTaxpayer>
              </RelevantTaxPayers>
            </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.noOfIntermediaries(xml).value mustBe 0
    }

  "must correctly validate intermediaries when intermediary discloser is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <Intermediaries>
              <Intermediary></Intermediary>
            </Intermediaries>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe true
  }

  "must correctly invalidate intermediaries when intermediary discloser is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe false
  }

  "must correctly validate intermediaries when intermediary discloser is not set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe true
  }

  "must correctly extract TaxpayerImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.taxPayerImplementingDates(xml).value mustBe Seq(
      new GregorianCalendar(2020, Calendar.MAY, 14).getTime,
      new GregorianCalendar(2020, Calendar.JUNE, 21).getTime,
    )
  }

  "must correctly invalidate mixed TaxpayerImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-06-26</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllTaxpayerImplementingDatesAreAfterStart()(xml).get.value mustBe false
  }

  "must correctly validate with TaxpayerImplementingDate equal to start" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-06-25</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllTaxpayerImplementingDatesAreAfterStart()(xml).get.value mustBe true
  }

  "must correctly extract ImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-01-21</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureInformationImplementingDates(xml).value mustBe Seq(
      new GregorianCalendar(2020, Calendar.JANUARY, 14).getTime,
      new GregorianCalendar(2018, Calendar.JANUARY, 21).getTime,
    )
  }

  "must correctly extract ImplementingDates when absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
          </DisclosureInformation>
          <DisclosureInformation>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureInformationImplementingDates(xml).value mustBe Seq.empty
  }

  "must correctly invalidate mixed ImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-01-21</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllImplementingDatesAreAfterStart()(xml).get.value mustBe false
  }

  "must correctly validate with ImplementingDate equal to start" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllImplementingDatesAreAfterStart()(xml).get.value mustBe true
  }

  "must correctly extract DisclosureImportInstruction" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureImportInstruction(xml).value mustBe "DAC6NEW"
  }

  "must correctly extract DisclosureID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureID(xml).value mustBe "AAA000000000"
  }

  "must correctly extract DisclosureID when not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureID(xml).value mustBe ""
  }

  "must correctly extract ArrangementID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.arrangementID(xml).value mustBe "AAA000000000"
  }

  "must correctly extract ArrangementID when not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.arrangementID(xml).value mustBe ""
  }

  "must correctly validate with New Disclosure without ArrangementID or DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly invalidate with New Disclosure with an ArrangementID but no DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with New Disclosure with no ArrangementID but a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with New Disclosure with ArrangementID and a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Add Disclosure with ArrangementID and no DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly invalidate with Add Disclosure with ArrangementID and DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with Add Disclosure with InitialDisclosureMA set to true" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
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

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstructionAndInitialDisclosureFlag()(xml).get.value mustBe false

  }

  "must correctly invalidate with Add Disclosure no ArrangementID but a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly extract MessageRefID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.messageRefID(xml).value mustBe "GB0000000XXX"
  }

  "must correctly validate with Rep Disclosure with ArrangementID and DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly validate with Rep Disclosure with ArrangementID no DisclosureID and with MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Rep Disclosure with ArrangementID and DisclosureID and no MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Rep Disclosure with no ArrangementID and with DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with ArrangementID and DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly validate with Del Disclosure with ArrangementID no DisclosureID and with MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with ArrangementID and DisclosureID and no MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with no ArrangementID and with DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate an initial disclosure MA with Relevant Tax Payers has a TaxPayer Implementation Dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate()(xml).get.value mustBe true
  }

  "must correctly invalidate an initial disclosure MA with Relevant Tax Payers has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate()(xml).get.value mustBe false
  }

  "must correctly validate an non initial disclosure MA with Relevant Tax Payers has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate()(xml).get.value mustBe true
  }

  "must correctly extract the hallmarks" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hallmarks(xml).value mustBe Seq("DAC6A1", "DAC6A3")
  }

  "must correctly extract is MainBenefitTest1" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <MainBenefitTest1>true</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasMainBenefitTest1(xml).value mustBe true
  }

  "must correctly extract is MainBenefitTest1 when not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasMainBenefitTest1(xml).value mustBe false
  }

  "must correctly validate the hallmarks when MainBenefitTest1 is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <MainBenefitTest1>true</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
                <Hallmark>DAC6C1c</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateInitialDisclosureMAWithRelevantTaxPayerHasImplementingDate()(xml).get.value mustBe true
  }

  "must correctly validate a file has TaxPayer Implementation Dates if initial disclosure MA is true, " +
    "first disclosure for arrangement ID is not found and Relevant Tax Payers exist" in {

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }
  }

  "must correctly validate a file has TaxPayer Implementation Dates if initial disclosure MA is false, " +
    "first disclosure's InitialDisclosureMA is true and Relevant Tax Payers exist" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New", initialDisclosureMA = true)

    when(mockCrossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(firstDisclosure))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }
  }

  "must correctly invalidate a file that has TaxPayer Implementation Dates if initial disclosure MA is false for DAC6NEW" in {

    val replacedFirstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "Replace", initialDisclosureMA = false)

    when(mockCrossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(replacedFirstDisclosure))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)
    }
  }

  "must correctly invalidate a DAC6ADD for a non-marketable arrangment where user has putTaxPayer Implementation Dates" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New", initialDisclosureMA = false)

    when(mockCrossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(firstDisclosure))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)
    }
  }

  "must correctly invalidate a DAC6REP for a non-marketable arrangement where user has putTaxPayer Implementation Dates" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New", initialDisclosureMA = false)

    when(mockCrossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(firstDisclosure))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)
    }
  }


  "must correctly invalidate a file with missing TaxPayer Implementation Dates if initial disclosure MA is false, " +
    "first disclosure's InitialDisclosureMA is true and Relevant Tax Payers exist" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New", initialDisclosureMA = true)

    when(mockCrossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(firstDisclosure))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe false
    }
  }

  "must correctly validate the hallmarks when MainBenefitTest1 is set and doesnt contain any of the necessary" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <MainBenefitTest1>true</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateMainBenefitTestHasASpecifiedHallmark()(xml).get.value mustBe true
  }

  "must correctly invalidate the hallmarks when MainBenefitTest1 is not set and doesnt contain any of the necessary hallmarks" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <MainBenefitTest1>false</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateMainBenefitTestHasASpecifiedHallmark()(xml).get.value mustBe false
  }

  "must correctly validate the hallmarks when MainBenefitTest1 is not set and doesnt contain any of the necessary" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateMainBenefitTestHasASpecifiedHallmark()(xml).get.value mustBe true
  }

  "must extract presence of DAC6D1OtherInfo" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasDAC6D1OtherInfo(xml).value mustBe true
  }

  "must extract absence of DAC6D1OtherInfo" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasDAC6D1OtherInfo(xml).value mustBe false
  }

  "must correctly validate that other info is provided when hallmark present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDAC6D1OtherInfoHasNecessaryHallmark()(xml).get.value mustBe true
  }

  "must recover from exception if implementing date is not in parseable format" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <ImplementingDate>wrong format</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must recover from exception if taxpayerImplementing date is not in parseable format" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <InitialDisclosureMA>true</InitialDisclosureMA>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <RelevantTaxpayerDiscloser>
                  <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                  <Capacity>DAC61105</Capacity>
                </RelevantTaxpayerDiscloser>
              </Liability>
              <RelevantTaxPayers>
                <RelevantTaxpayer>
                  <TaxpayerImplementingDate>wrong format</TaxpayerImplementingDate>
                </RelevantTaxpayer>
              </RelevantTaxPayers>
            </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must correctly invalidate that other info is provided when hallmark absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C4</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDAC6D1OtherInfoHasNecessaryHallmark()(xml).get.value mustBe false
  }
  "must return correct errors for xml with mutiple errors: " +
    " error 1 = initial disclosure marketable arrangement does not have one or more relevant taxpayers " +
    " error 2 = other info is provided when hallmark absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <Hallmarks>
            <ListHallmarks>
              <Hallmark>DAC6C4</Hallmark>
            </ListHallmarks>
            <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
          </Hallmarks>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List(Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false),
        Validation("businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo", false))
    }
  }

  "must return no errors for valid xml" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must return correct metadata for import instruction DAC6NEW when disclosure info is present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
        <InitialDisclosureMA>true</InitialDisclosureMA>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6NEW", None, None,
                                                    disclosureInformationPresent = true, initialDisclosureMA = true))
  }

  "must return correct metadata for import instruction DAC6NEW  when disclosure info is not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <DAC6Disclosures>
        </DAC6Disclosures>
      </DAC6_Arrangement>
    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6NEW", None, None,
                                               disclosureInformationPresent = false, initialDisclosureMA = false))
  }

  "must return correct metadata for import instruction DAC6ADD" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
                                                 disclosureInformationPresent = true, initialDisclosureMA = false))
  }

  "must return correct metadata for import instruction DAC6ADD with RelevantTaxpayers who all have implementing dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
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

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
                                                    disclosureInformationPresent = true, initialDisclosureMA = false))
  }


  "must return correct metadata for import instruction DAC6ADD with RelevantTaxpayers who do not all have implementing dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
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

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
                                                 disclosureInformationPresent = true, initialDisclosureMA = false))
  }

  "must return correct metadata for import instruction DAC6REP" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6REP", Some("AAA000000000"), Some("AAA000000000"),
                                                   disclosureInformationPresent = true, initialDisclosureMA = false))
  }

  "must return correct metadata for import instruction DAC6DEL" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6DEL", Some("AAA000000000"), Some("AAA000000000"),
                                                     disclosureInformationPresent = true, initialDisclosureMA = false))
  }
  "must throw exception if disclosureImportInstruction is invalid or missing" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    a[RuntimeException] mustBe thrownBy {
      service.extractDac6MetaData()(xml)
    }
  }

}

