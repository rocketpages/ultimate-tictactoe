package shared

object ClientToServerProtocol {

  type MessageType = String
  sealed trait Payload

  case class ClientToServerWrapper(t: MessageType, p: Payload)

  case class TurnCommand(gameId: Int, gridId: Int) extends Payload
  case class CreateGameCommand(name: String) extends Payload
  case class JoinGameCommand(uuid: String, name: String) extends Payload

  def wrapTurnCommand(p: TurnCommand) = ClientToServerWrapper(new MessageType(MessageKeyConstants.MESSAGE_TURN_COMMAND), p)
  def wrapJoinGameCommand(p: JoinGameCommand) = ClientToServerWrapper(new MessageType(MessageKeyConstants.MESSAGE_JOIN_GAME_COMMAND), p)
  def wrapCreateGameCommand(p: CreateGameCommand) = ClientToServerWrapper(new MessageType(MessageKeyConstants.MESSAGE_CREATE_GAME_COMMAND), p)

}