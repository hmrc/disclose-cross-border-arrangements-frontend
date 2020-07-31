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

import scala.util.{Success, Try}


object ErrorMessageHelper {

  def transformErrorMessage(errorMessage:String, errorType: Option[String],
                            elementName: Option[String], subType: Option[String]): String = {

    getErrorMessage(errorMessage, errorType, elementName, subType) match {
      case Some(message) => message
      case None => "There is something wrong with this line"
    }
  }

  private def missingInfoMessage(elementName: String): String = {
    val vowels = "aeiouAEIOU"
    if(vowels.contains(elementName.head)){
      s"Enter an $elementName"
    }else s"Enter a $elementName"

  }

   def invalidCodeMessage(elementName: String, subType: Option[String]): Option[String] = {
     println("in here " + elementName)
     println("in here " + subType)
    elementName match {
      case "Country" | "CountryExemption" | "TIN issuedBy" => Some(s"$elementName is not one of the ISO country codes")
      case "ConcernedMS" => Some("ConcernedMS is not one of the ISO EU Member State country codes")
      case "Reason" | "IntermediaryNexus" | "RelevantTaxpayerNexus" |
           "Hallmark" | "ResCountryCode"  => Some(s"$elementName is not one of the allowed values")
      case "Capacity" => Some(s"${subType.get}/Capacity is not one of the allowed values")
      case _ => None
    }
   }

  def getErrorMessage(errorMessage: String, errorType: Option[String],
                      elementName: Option[String], subType: Option[String]): Option[String] ={

   errorType match{
     case Some("cvc-minLen") => Some(missingInfoMessage(elementName.get))
     case Some("cvc-maxLen") => Some(elementName.get + s" must be ${subType.get} characters or less")
     case Some("cvc-enumer") => invalidCodeMessage(elementName.get, subType)//Some(elementName.get + s" is not one of the ISO country codes")
     case Some("missingAttribute") => getMissingAttributeName(errorMessage)
     case Some("invalidAttribute") => Some("Amount currCode is not one of the ISO currency codes")//getMissingAttributeName(errorMessage)
     case _ =>   None
   }

  }

  def getMissingAttributeName(errorMessage:String): Option[String] = {

   // "cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'."
    val splitErrorMessage = errorMessage.split(" ")

    val elementName = Try {
      val elementName = splitErrorMessage(7)
      val attributeName = splitErrorMessage(2)
      elementName.substring(1, elementName.length - 2) + " " + attributeName.substring(1, attributeName.length - 1)
    }

    elementName match {
      case Success(name) => Some(missingInfoMessage(name))
      case _ => None
    }

  }




 // def getMaxLengthElementName(errorMessage:String): Option[String] = {
    //
    //    val elementName = Try {
    //      errorMessage.split(" ").last.dropRight(1)
    //    }
    //    elementName match {
    //      case Success(name) => Some(name + " must be 400 characters or less")
    //      case _ => None
    //    }
    //
    //  }

  //  def determineErrorType(errorMessage: String):String ={
  //
  //    if(errorMessage.contains("cvc-minLen")){
  //      "missingMandatoryInfo"
  //    }else {
  //      if(errorMessage.startsWith("cvc-maxLen")){
  //        "maxLengthExceeded"
  //      }else "missingAttribute"
  //    }
  //   }

  //
  //  def getMissingElementName(errorMessage:String): Option[String] = {
  //
  //    val elementName = Try {
  //      errorMessage.split(" ").last.dropRight(1)
  //    }
  //    elementName match {
  //      case Success(name) => Some(missingInfoMessage(name))
  //      case _ => None
  //    }
  //
  //  }

}
