package backend.messages

import play.api.libs.json._
import play.api.libs.functional.syntax._

object OpponentTurnResponse {
  val RESPONSE: String = "response"

  var MESSAGE_YOU_WIN = "YOU_WIN";
  var MESSAGE_TIED = "TIED";
  var MESSAGE_YOUR_TURN = "YOUR_TURN";

  implicit val writes: Writes[OpponentTurnResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "gridId").write[String] and
    (JsPath \ "status").write[String])(unlift(OpponentTurnResponse.unapply))
}

case class OpponentTurnResponse(messageType: String = OpponentTurnResponse.RESPONSE, gridId: String, status: String)

