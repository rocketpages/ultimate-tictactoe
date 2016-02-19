package actors.game

import akka.event.Logging
import model.akka.ActorMessageProtocol.RegisterPlayerRequest
import akka.actor._

import scala.collection.mutable.Queue

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  val log = Logging(context.system, this)

  var openGameQ = new Queue[ActorRef]

  def receive = {
    // find open game and register player for the game
    case r: RegisterPlayerRequest => findGame(r) ! r
    case _ => log.error("Invalid type in receive")
  }

  private def findGame(r: RegisterPlayerRequest) = {
    if (openGameQ.isEmpty) {
      // create a new game and add it to the queue
      val newGame = {
        val gameUuid = java.util.UUID.randomUUID.toString
        // TODO, monitor through DeathWatch?
        context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
      }
      openGameQ += newGame
      newGame
    } else {
      // pull a game off the queue that is waiting for players
      openGameQ.dequeue
    }
  }
}
