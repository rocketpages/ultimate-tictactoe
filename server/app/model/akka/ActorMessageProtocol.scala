package model.akka

import actors.PlayerLetter._
import akka.actor.ActorRef

object ActorMessageProtocol {

  case class StartGameMessage(turnIndicator: String, playerLetter: PlayerLetter, game: ActorRef)
  case class RegisterPlayerWithEngine(player: ActorRef)
  case class RegisterPlayerWithGame(uuid: String, player: ActorRef)
  case class TurnRequest(playerLetter: PlayerLetter, game: String, grid: String, playerX: Option[ActorRef] = None, playerO: Option[ActorRef] = None)
  case class RegisterGameStreamSubscriber()
  case class UpdateSubscribersWithGameStatus(subscribers: List[ActorRef])

}