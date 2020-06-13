package services

import javax.inject.Inject
import models.{Add, ImportInstruction, New, Replace}

class IDService @Inject()() {

  def generateIDsForInstruction(importInstruction: ImportInstruction) = {
    importInstruction match {
      case New => ???
      case Add => ???
      case Replace => ???
    }
  }


}
