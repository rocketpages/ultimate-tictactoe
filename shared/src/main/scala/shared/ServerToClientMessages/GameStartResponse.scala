package shared.ServerToClientMessages

object GameStartResponse {
  val TURN: String = "turn"
  val YOUR_TURN: String = "YOUR_TURN"
  val WAITING: String = "WAITING"
}

case class GameStartResponse(messageType: String = GameStartResponse.TURN, turnIndicator: String, playerLetter: String)

