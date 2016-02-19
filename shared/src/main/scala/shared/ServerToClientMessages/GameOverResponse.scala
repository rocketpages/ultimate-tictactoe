package shared.ServerToClientMessages

object GameOverResponse {
  val GAME_OVER: String = "GAME_OVER"
}

case class GameOverResponse(tied: Boolean,
                            lastMovePlayer: String,
                            lastGameId: Int,
                            lastGridId: Int,
                            messageType: String = GameOverResponse.GAME_OVER) {
  val lastGridIdSelector = "cell_" + lastGameId + lastGridId
}


