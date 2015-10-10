package model.json

import play.api.libs.functional.syntax._
import play.api.libs.json._

object OpponentTurnResponse {
  val RESPONSE: String = "response"

  var MESSAGE_YOU_WIN = "YOU_WIN";
  var MESSAGE_TIED = "TIED";
  var MESSAGE_YOUR_TURN = "YOUR_TURN";

  implicit val writes: Writes[OpponentTurnResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "gameId").write[String] and
    (JsPath \ "gridId").write[String] and
    (JsPath \ "nextGameId").write[String] and
    (JsPath \ "lastBoardWon").write[Boolean] and
    (JsPath \ "allBoardsWon").write[Array[String]] and
    (JsPath \ "status").write[String])(unlift(OpponentTurnResponse.unapply))
}

case class OpponentTurnResponse(
                                 messageType: String = OpponentTurnResponse.RESPONSE,
                                 gameId: String,
                                 gridId: String,
                                 nextGameId: String,
                                 lastBoardWon: Boolean,
                                 allBoardsWon: Array[String],
                                 status: String)

