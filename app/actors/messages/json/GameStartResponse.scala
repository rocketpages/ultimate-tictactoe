package actors.messages.json

import akka.actor.ActorRef
import play.api.libs.functional.syntax._
import play.api.libs.json._

object GameStartResponse {
  val TURN: String = "turn"
  val YOUR_TURN: String = "YOUR_TURN"
  val WAITING: String = "WAITING"

  implicit val writes: Writes[GameStartResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "turnIndicator").write[String] and
    (JsPath \ "playerLetter").write[String])(unlift(GameStartResponse.unapply))
}

case class GameStartResponse(messageType: String = GameStartResponse.TURN, turnIndicator: String, playerLetter: String)

