package model.akka

import actors.PlayerLetter._
import akka.actor.ActorRef

object ActorMessageProtocol {

  case class StartGameMessage(turnIndicator: String, playerLetter: PlayerLetter, game: ActorRef, nameX: String, nameO: String)
  case class TurnMessage(playerLetter: PlayerLetter, game: String, grid: String, playerX: Option[ActorRef] = None, playerO: Option[ActorRef] = None)
  case class RegisterGameStreamSubscriberMessage()
  case class UpdateSubscribersWithGameStatusMessage(subscribers: List[ActorRef])
  case class CreateGameMessage(player: ActorRef, name: String)
  case class JoinGameMessage(player: ActorRef, name: String, uuid: String)
  case class RegisterPlayerWithGameMessage(uuid: String, player: ActorRef, name: String)
  case class GameWonMessage(lastPlayer: String, lastGameBoardPlayed: Int, lastGridPlayed: Int)
  case class GameTiedMessage(lastPlayer: String, lastGameBoardPlayed: Int, lastGridPlayed: Int)

}