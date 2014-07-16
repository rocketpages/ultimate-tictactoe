package actors

import actors.messages.RegisterPlayerRequest
import akka.actor._

object GamesActor {
  def props = Props(new GamesActor)
}

class GamesActor extends Actor {
  def receive = {
    case registerPlayer: RegisterPlayerRequest => {
      val player = registerPlayer.player
      val uuid = registerPlayer.uuid

      // 1. Search for an open game first (this won't work!)
      val game = context.actorOf(Props[GameActor], name = "gameActor")

      // 2. When you find an open game or create a new one, register the player
      game ! registerPlayer
    }
  }
}

