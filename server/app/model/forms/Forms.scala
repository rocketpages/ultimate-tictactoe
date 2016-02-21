package model.forms

import play.api.data.Form
import play.api.data.Forms._

object Forms {

  case class GameData(name: String)
  val gameForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(GameData.apply)(GameData.unapply)
  )

}