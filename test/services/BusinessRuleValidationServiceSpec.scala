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

import java.util.{Calendar, GregorianCalendar}

import base.SpecBase
import fixtures.XMLFixture
import org.scalatest.concurrent.IntegrationPatience

class BusinessRuleValidationServiceSpec extends SpecBase with IntegrationPatience {

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

}
