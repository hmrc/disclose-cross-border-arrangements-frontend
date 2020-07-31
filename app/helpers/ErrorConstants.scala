package helpers

trait ErrorConstants {


  val ERROR_TYPE_PATTERN = """cvc-maxLength-valid"""
  val MAX_LENGTH_PATTERN = """ maxLength ('[0-9]{3,4}')"""

  val INVALID_ATTRIBUTE_ERROR = """cvc-attribute.3"""
  val MISSING_ATTRIBUTE_ERROR = """cvc-complex-type.4"""
  val MAX_LENGTH_ERROR = "cvc-maxLength-valid"
  val MISSING_VALUE_ERROR = "cvc-minLength-valid"
  val INVALID_ENUM_ERROR = "cvc-enumeration-valid"

  val ERROR_TYPES = List (MISSING_ATTRIBUTE_ERROR, INVALID_ATTRIBUTE_ERROR, INVALID_ENUM_ERROR, MAX_LENGTH_ERROR, MISSING_VALUE_ERROR)


}

