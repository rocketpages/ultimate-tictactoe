package shared

object ServerToClientProtocol {

  sealed trait Payload

  case class BoardWonResponse(gameId: String) extends Payload

  case class GameStartResponse(turnIndicator: String, playerLetter: String, nameX: String, nameO: String) extends Payload

  case class HandshakeResponse(status: String) extends Payload

  case class Ping() extends Payload

  case class OpponentTurnResponse(gameId: Int, gridId: Int, nextGameId: Int, lastBoardWon: Boolean, boardsWonArr: Array[String], status: String) extends Payload {
    val gridIdSelector = "cell_" + gameId + gridId
  }

  case class GameOverResponse(tied: Boolean, lastMovePlayer: String, lastGameId: Int, lastGridId: Int) extends Payload {
    val lastGridIdSelector = "cell_" + lastGameId + lastGridId
  }

  case class GameCreatedEvent(uuid: String, x: String) extends Payload
  case class GameStartedEvent(uuid: String, x: String, o: String) extends Payload

  type MessageType = String

  case class ServerToClientWrapper(t: MessageType, p: Payload)

  def wrapBoardWonResponse(m: BoardWonResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_BOARD_WON), m)
  def wrapGameStartResponse(m: GameStartResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STARTED), m)
  def wrapHandshakeResponse(m: HandshakeResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_HANDSHAKE), m)
  def wrapPing(m: Ping) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_KEEPALIVE), m)
  def wrapOpponentTurnResponse(m: OpponentTurnResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE), m)
  def wrapGameOverResponse(m: GameOverResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_OVER), m)

  def wrapGameCreatedEvent(m: GameCreatedEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_NEW_GAME_CREATED_EVENT), m)
  def wrapGameStartedEvent(m: GameStartedEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STARTED_EVENT), m)
}