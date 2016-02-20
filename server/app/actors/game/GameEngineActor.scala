package actors.game

import akka.event.Logging
import model.akka.ActorMessageProtocol._
import akka.actor._
import scala.collection.mutable.ListBuffer

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  val log = Logging(context.system, this)

  var games = scala.collection.mutable.HashMap.empty[String, ActorRef]
  var subscribers = new ListBuffer[ActorRef]

  def receive = {
    // find open game and register player for the game
    case c: JoinGameMessage => {
      // register player with the game
      val g = games(c.uuid)
      g ! RegisterPlayerWithGameMessage(c.uuid, c.player, c.name)
    }
    case c: CreateGameMessage => {
      // create the game
      val uuid = java.util.UUID.randomUUID.toString
      val newGame = {
        context.actorOf(Props[GameActor], name = "gameActor" + uuid)
      }
      games += (uuid -> newGame)
      newGame ! RegisterPlayerWithGameMessage(uuid, c.player, c.name)

      // update all subscribers of this new game
      games.values.foreach(g => g ! UpdateSubscribersWithGameStatus(subscribers.toList))
    }
    // register the sender as a subscriber to game updates
    case RegisterGameStreamSubscriber => {
      subscribers += sender()
      games.values.foreach(g => {
        g ! UpdateSubscribersWithGameStatus(subscribers.toList)
      })
    }
    case x => log.error("Invalid type in receive - ", x)
  }
}
