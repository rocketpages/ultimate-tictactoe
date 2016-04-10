package model.akka

import actors.PlayerLetter._
import actors.game.GameActor
import akka.actor.ActorRef

object ActorMessageProtocol {

  case class StartGameMessage(turnIndicator: String, playerLetter: PlayerLetter, game: ActorRef, nameX: String, nameO: String)
  case class TurnMessage(playerLetter: PlayerLetter, game: String, grid: String)
  case class ProcessNextTurnMessage(playerLetter: PlayerLetter, game: String, grid: String, x: ActorRef, o: ActorRef, xTurns: Int, oTurns: Int)
  case class RegisterGameStreamSubscriberMessage()
  case class UpdateSubscribersWithGameStatusMessage(subscribers: List[ActorRef])
  case class CreateGameMessage(player: ActorRef, name: String)
  case class JoinGameMessage(player: ActorRef, name: String, uuid: String)
  case class RegisterPlayerWithGameMessage(uuid: String, player: ActorRef, name: String)
  case class GameWonMessage(lastPlayer: String, lastGameBoardPlayed: Int, lastGridPlayed: Int)
  case class GameTiedMessage(lastPlayer: String, lastGameBoardPlayed: Int, lastGridPlayed: Int)
  case class PlayAgainMessage(player: String, playAgain: Boolean)
  case class GameOverMessage(uuid: String, fromPlayer: String)
  case class GameTerminatedMessage(terminatedByPlayer: String)
  case class GameCreatedMessage(gameActor: ActorRef, playerLetter: PlayerLetter)
  case class GameWonSubscriberUpdateMessage(uuid: String, winsPlayerX: Int, winsPlayerO: Int, totalGamesPlayed: Int)
  case class GameTiedSubscriberUpdateMessage(uuid: String, totalGamesPlayed: Int)
  case class SendGameStreamUpdateCommand()
  case class OpenGameStreamUpdateMessage(uuid: String, xName: String)
  case class ClosedGameStreamUpdateMessage(uuid: String, xName: String, oName: String, xWins: Int, oWins: Int, totalGames: Int)
  case class GameStreamTurnUpdateMessage(uuid: String, xTurns: Int, oTurns: Int)
  case class GameStreamGameStartedMessage(uuid: String, xName: String, oName: String)

}