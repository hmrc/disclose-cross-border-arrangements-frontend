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

package utils

import base.SpecBase
import org.scalatest.StreamlinedXmlEquality

class SubmissionUtilSpec extends SpecBase with StreamlinedXmlEquality {

  "Submission Util" - {
    "must construct a submission document from a fileName and document" in {
      val xml =
        <test>
          <value>This should be preserved</value>
        </test>

      val expectedReturn =
        <submission>
          <fileName>test-file.xml</fileName>
          <enrolmentID>enrolmentID</enrolmentID>
          <file>
            <test>
              <value>This should be preserved</value>
            </test>
          </file>
        </submission>

      SubmissionUtil.constructSubmission("test-file.xml", "enrolmentID", xml) mustEqual expectedReturn
    }
  }

}
