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

}
