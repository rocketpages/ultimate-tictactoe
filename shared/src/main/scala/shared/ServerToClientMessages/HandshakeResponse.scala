package shared.ServerToClientMessages

object HandshakeResponse {
  val HANDSHAKE: String = "handshake"
  val OK: String = "ok"

  def apply(s: String) = new HandshakeResponse(status = s)
}

case class HandshakeResponse(messageType: String = HandshakeResponse.HANDSHAKE, status: String)

