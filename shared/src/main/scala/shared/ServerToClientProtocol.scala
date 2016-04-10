package shared

object ServerToClientProtocol {

  sealed trait Payload

  case class BoardWonResponse(gameId: String) extends Payload

  case class GameStartResponse(turnIndicator: String, playerLetter: String, nameX: String, nameO: String) extends Payload

  case class HandshakeResponse(status: String) extends Payload

  case class Ping() extends Payload

  case class OpponentTurnResponse(gameId: Int, gridId: Int, nextGameId: Int, lastBoardWon: Boolean, boardsWonArr: Array[String], status: String, xTurns: Int, oTurns: Int) extends Payload {
    val gridIdSelector = "cell_" + gameId + gridId
  }

  case class GameWonResponse(lastPlayer: String, lastGameId: Int, lastGridId: Int, totalGames: Int, winsX: Int, winsO: Int) extends Payload {
    val lastGridIdSelector = "cell_" + lastGameId + lastGridId
  }

  case class GameLostResponse(lastPlayer: String, lastGameId: Int, lastGridId: Int, totalGames: Int, winsX: Int, winsO: Int) extends Payload {
    val lastGridIdSelector = "cell_" + lastGameId + lastGridId
  }

  case class GameTiedResponse(lastPlayer: String, lastGameId: Int, lastGridId: Int, totalGames: Int) extends Payload {
    val lastGridIdSelector = "cell_" + lastGameId + lastGridId
  }

  case class GameCreatedEvent(uuid: String, x: String) extends Payload

  case class GameStartedEvent(uuid: String, x: String, o: String) extends Payload

  case class GameOverEvent(uuid: String, fromPlayer: String) extends Payload

  case class GameStreamWonEvent(uuid: String, winsPlayerX: Int, winsPlayerO: Int, totalGames: Int) extends Payload

  case class GameStreamTiedEvent(uuid: String, totalGames: Int) extends Payload

  case class OpenGameStreamUpdateEvent(uuid: String, xName: String) extends Payload

  case class ClosedGameStreamUpdateEvent(uuid: String, xName: String, oName: String, xWins: Int, oWins: Int, totalGames: Int) extends Payload

  case class GameStreamTurnEvent(uuid: String, xTurns: Int, oTurns: Int) extends Payload

  type MessageType = String

  case class ServerToClientWrapper(t: MessageType, p: Payload)

  def wrapBoardWonResponse(m: BoardWonResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_BOARD_WON), m)
  def wrapGameStartResponse(m: GameStartResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STARTED), m)
  def wrapHandshakeResponse(m: HandshakeResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_HANDSHAKE), m)
  def wrapPing(m: Ping) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_KEEPALIVE), m)
  def wrapOpponentTurnResponse(m: OpponentTurnResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE), m)
  def wrapGameLostResponse(m: GameLostResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_LOST), m)
  def wrapGameWonResponse(m: GameWonResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_WON), m)
  def wrapGameTiedResponse(m: GameTiedResponse) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_TIED), m)
  def wrapGameCreatedEvent(m: GameCreatedEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_NEW_GAME_CREATED_EVENT), m)
  def wrapGameStartedEvent(m: GameStartedEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STARTED_EVENT), m)
  def wrapGameOverEvent(m: GameOverEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_OVER), m)
  def wrapGameStreamWonEvent(m: GameStreamWonEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STREAM_WON_EVENT), m)
  def wrapGameStreamTiedEvent(m: GameStreamTiedEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STREAM_TIED_EVENT), m)
  def wrapOpenGameStreamUpdateEvent(m: OpenGameStreamUpdateEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_OPEN_GAME_STREAM_UPDATE_EVENT), m)
  def wrapClosedGameStreamUpdateEvent(m: ClosedGameStreamUpdateEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_CLOSED_GAME_STREAM_UPDATE_EVENT), m)
  def wrapGameStreamTurnEvent(m: GameStreamTurnEvent) = ServerToClientWrapper(new MessageType(MessageKeyConstants.MESSAGE_GAME_STREAM_TURN_EVENT), m)

}