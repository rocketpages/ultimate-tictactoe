package actors

import actors.GameStatus._
import akka.actor._

object GamesActor {
  def props() = Props(new GamesActor)
}

class GamesActor extends Actor {

  import actors.GameStatus._

  // Status of the current game
  var gameStatus: GameStatus = WAITING

  def receive = {
    case msg: String => sender() ! ("I received your message: " + msg)
  }

}

