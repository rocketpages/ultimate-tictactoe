package actors

import actors.messages.{ RegisterPlayerResponse, RegisterPlayerRequest }
import akka.actor._
import backend.messages.TurnResponse

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  var playerX: Option[ActorRef] = None
  var playerO: Option[ActorRef] = None

  // Status of the current game
  var gameStatus: GameStatus = WAITING

  def receive = {
    case registerPlayerRequest: RegisterPlayerRequest => {
      val player = registerPlayerRequest.player
      val letter = registerPlayer(player)
      sender ! RegisterPlayerResponse(self, letter)

      // Start the game!
      if (playerX != None && playerO != None) {
        playerX.get ! TurnResponse(turnIndicator = TurnResponse.YOUR_TURN)
        playerO.get ! TurnResponse(turnIndicator = TurnResponse.WAITING)
      }
    }
  }

  private def registerPlayer(player: ActorRef): Option[PlayerLetter.PlayerLetter] = {
    if (playerX == None) {
      playerX = Some(player)
      Some(PlayerLetter.X)
    } else if (playerO == None) {
      playerO = Some(player)
      Some(PlayerLetter.O)
    } else {
      None
    }
  }

}
