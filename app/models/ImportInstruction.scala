package models

sealed trait ImportInstruction

case object New extends ImportInstruction
case object Add extends ImportInstruction
case object Replace extends ImportInstruction

object ImportInstruction {
  def apply(name: String): ImportInstruction = name match {
    case "DAC6NEW" => New
    case "DAC6ADD" => Add
    case "DAC6REP" => Replace
    //case _ => new IllegalArgumentException("Unrecognised Import Instruction")
  }
}