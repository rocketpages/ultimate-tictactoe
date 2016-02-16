package model.json

import play.api.libs.functional.syntax._
import play.api.libs.json._

object BoardWonResponse {
  val MESSAGE_TYPE: String = "board_won"

  implicit val writes: Writes[BoardWonResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "gameId").write[String])(unlift(BoardWonResponse.unapply))
}

case class BoardWonResponse(messageType: String = BoardWonResponse.MESSAGE_TYPE, gameId: String)

