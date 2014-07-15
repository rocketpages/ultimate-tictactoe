package actors

import akka.actor._

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  // Status of the current game
  var gameStatus: GameStatus = WAITING

  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }

}
