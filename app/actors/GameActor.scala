package actors

import actors.messages.{RegisterPlayerResponse, RegisterPlayerRequest}
import akka.actor._

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  val UUID = java.util.UUID.randomUUID.toString

  var playerX: Option[ActorRef] = None
  var playerO: Option[ActorRef] = None

  // Status of the current game
  var gameStatus: GameStatus = WAITING

  def receive = {
    case registerPlayerRequest: RegisterPlayerRequest => {
      val player = registerPlayerRequest.player
      val letter = registerPlayer(player)
      player ! RegisterPlayerResponse(self, letter)
    }
  }

  private def registerPlayer(player: ActorRef) = {
    if (playerX == None) {
      playerX = Some(player)
      PlayerLetter.X
    }
    else {
      playerO = Some(player)
      PlayerLetter.O
    }
  }

}
