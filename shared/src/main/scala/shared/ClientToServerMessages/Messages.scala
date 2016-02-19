package ClientToServerMessages

object Messages {
  case class TurnMessage(gridId: String) {
    val messageType: String = "TURN"
  }

  case class RegisterGameRequest() {
    val messageType: String = "REGISTER_GAME_REQUEST"
  }
}