package backend.messages

import actors.PlayerLetter.PlayerLetter
import play.api.libs.json._
import play.api.libs.functional.syntax._

object HandshakeResponse {
  val HANDSHAKE: String = "handshake"

  implicit val writes: Writes[HandshakeResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "playerLetter").write[PlayerLetter])(unlift(HandshakeResponse.unapply))

  def apply(l: PlayerLetter) = new HandshakeResponse(playerLetter = l)
}

case class HandshakeResponse(messageType: String = HandshakeResponse.HANDSHAKE, playerLetter: PlayerLetter)

