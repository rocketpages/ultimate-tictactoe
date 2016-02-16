package model.json

import actors.PlayerLetter.PlayerLetter
import play.api.libs.functional.syntax._
import play.api.libs.json._

object HandshakeResponse {
  val HANDSHAKE: String = "handshake"
  val OK: String = "ok"

  implicit val writes: Writes[HandshakeResponse] = (
    (JsPath \ "messageType").write[String] and
    (JsPath \ "status").write[String])(unlift(HandshakeResponse.unapply))

  def apply(s: String) = new HandshakeResponse(status = s)
}

case class HandshakeResponse(messageType: String = HandshakeResponse.HANDSHAKE, status: String)

