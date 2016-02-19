package shared

object ClientToServerProtocol {

  type MessageType = String
  sealed trait Payload

  case class ClientToServerWrapper(t: MessageType, p: Payload)

  case class TurnCommand(gameId: Int, gridId: Int) extends Payload
  case class RegisterGameCommand() extends Payload

  def wrapTurnCommand(p: TurnCommand) = ClientToServerWrapper(new MessageType(MessageKeyConstants.MESSAGE_TURN_COMMAND), p)
  def wrapRegisterGameCommand(p: RegisterGameCommand) = ClientToServerWrapper(new MessageType(MessageKeyConstants.MESSAGE_REGISTER_COMMAND), p)

}