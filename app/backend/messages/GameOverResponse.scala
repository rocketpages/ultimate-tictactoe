package backend.messages

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

object GameOverResponse {
  val GAME_OVER: String = "GAME_OVER"

  implicit val writes: Writes[GameOverResponse] = (
    (JsPath \ "tied").write[Boolean] and
      (JsPath \ "winningPlayer").writeNullable[String] and
      (JsPath \ "lastGridId").write[String] and
      (JsPath \ "messageType").write[String])(unlift(GameOverResponse.unapply))
}

case class GameOverResponse(tied: Boolean, winningPlayer: Option[String], lastGridId: String, messageType: String = GameOverResponse.GAME_OVER)


