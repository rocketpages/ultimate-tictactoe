package actors

import actors.PlayerLetter.PlayerLetter
import akka.actor._

object PlayerActor {
  def props(channel: ActorRef) = Props(new PlayerActor(channel))
}

class PlayerActor(channel: ActorRef) extends Actor {

  //1. find game, e.g, val game: Game = findGame()
  //2. assign letter, e.g, val letter: Option[PlayerLetter] = game.addPlayer(self)
  //3. start game if two people present

  override def preStart() {

  }

  def receive = {
    case msg: String =>
      channel ! ("I received your message: " + msg)
  }

}
