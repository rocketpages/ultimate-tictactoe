package shared.ServerToClientMessages

object OpponentTurnResponse {
  val RESPONSE: String = "response"

  var MESSAGE_YOU_WIN = "YOU_WIN"
  var MESSAGE_TIED = "TIED"
  var MESSAGE_YOUR_TURN = "YOUR_TURN"
}

case class OpponentTurnResponse(
                                 messageType: String = OpponentTurnResponse.RESPONSE,
                                 gameId: Int,
                                 gridId: Int,
                                 nextGameId: Int,
                                 lastBoardWon: Boolean,
                                 boardsWonArr: Array[String],
                                 status: String) {
  val gridIdSelector = "cell_" + gameId + gridId
}

