package shared.ServerToClientMessages

object BoardWonResponse {
  val MESSAGE_TYPE: String = "board_won"
}

case class BoardWonResponse(messageType: String = BoardWonResponse.MESSAGE_TYPE, gameId: String)

