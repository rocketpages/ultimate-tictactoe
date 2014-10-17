package actors.messages

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

object GameOverPlayerResponse {

  val GAME_OVER: String = "GAME_OVER"
  val YOU_WIN: String = "YOU_WIN"
  val YOU_LOSE: String = "YOU_LOSE"
  val TIED: String = "TIED"

  implicit val writes: Writes[GameOverPlayerResponse] = (
    (JsPath \ "messageType").write[String] and
      (JsPath \ "status").write[String])(unlift(GameOverPlayerResponse.unapply))

}

case class GameOverPlayerResponse(messageType: String = "GAME_OVER", winner: String)
