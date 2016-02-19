package model.akka

import actors.GameStatus._
import actors.PlayerLetter._
import akka.actor.ActorRef

object ActorMessageProtocol {

  case class StartGameMessage(turnIndicator: String, playerLetter: PlayerLetter, game: ActorRef)
  case class RegisterPlayerRequest(player: ActorRef)
  //case class RegisterPlayerResponse(status: String, game: ActorRef, playerLetter: Option[PlayerLetter] = None)
  //case class StartGameRequest()
  case class TurnRequest(playerLetter: PlayerLetter, game: String, grid: String, playerX: Option[ActorRef] = None, playerO: Option[ActorRef] = None)
  //case class TurnResponse(playerLetter: PlayerLetter, gameStatus: GameStatus)

}