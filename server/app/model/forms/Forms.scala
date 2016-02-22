package model.forms

import play.api.data.Form
import play.api.data.Forms._

object Forms {

  case class GameData(nameX: String)
  val gameForm = Form(
    mapping(
      "nameX" -> nonEmptyText
    )(GameData.apply)(GameData.unapply)
  )

  case class JoinGameData(nameX: String, nameO: String, uuid: String)
  val joinGameForm = Form(
    mapping(
      "nameX" -> nonEmptyText,
      "nameO" -> nonEmptyText,
      "uuid" -> nonEmptyText
    )(JoinGameData.apply)(JoinGameData.unapply)
  )

}