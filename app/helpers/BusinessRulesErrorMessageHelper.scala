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

package helpers

import models.{GenericError, Validation}

import scala.xml.{Elem, NodeSeq}

class BusinessRulesErrorMessageHelper {

  def convertToGenericErrors(validations: Seq[Validation], xml: Elem): Seq[GenericError] = {
    val xmlArray = xml.toString().split("\n")

    val valsWithLineNumber =  validations.map(validation => validation.setLineNumber(xmlArray))

   valsWithLineNumber.map(validation => validation.toGenericError)




  }

 }

