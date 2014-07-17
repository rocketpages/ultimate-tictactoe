package backend.messages

import play.api.libs.json._
import play.api.libs.functional.syntax._

object TurnResponse {
  val TURN: String = "turn"
  val YOUR_TURN: String = "YOUR_TURN"
  val WAITING: String = "WAITING"

  implicit val writes: Writes[TurnResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "turnIndicator").write[String])(unlift(TurnResponse.unapply))
}

case class TurnResponse(messageType: String = TurnResponse.TURN, turnIndicator: String)

