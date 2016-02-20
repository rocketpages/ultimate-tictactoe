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

  var openGames = scala.collection.mutable.HashMap.empty[String, ActorRef]
  var subscribers = new ListBuffer[ActorRef]

  def receive = {
    // find open game and register player for the game
    case r: RegisterPlayerWithEngine => {
      val (uuid, game) = findGame(r)
      game ! RegisterPlayerWithGame(uuid, r.player)

      // update all subscribers of this new game
      openGames.toList.foreach(g => {
        g._2 ! UpdateSubscribersWithGameStatus(subscribers.toList)
      })
    }
    // register the sender as a subscriber to game updates
    case RegisterGameStreamSubscriber => {
      subscribers += sender()
      openGames.toList.foreach(g => {
        g._2 ! UpdateSubscribersWithGameStatus(subscribers.toList)
      })
    }
    case x => log.error("Invalid type in receive - ", x)
  }

  private def findGame(r: RegisterPlayerWithEngine) = {
    if (openGames.isEmpty) {
      // create a new game and add it to the queue
      val uuid = java.util.UUID.randomUUID.toString
      val newGame = {
        context.actorOf(Props[GameActor], name = "gameActor" + uuid)
      }
      openGames += (uuid -> newGame)
      (uuid, newGame)
    } else {
      // pull a game off the queue that is waiting for players
      val g = openGames.head
      openGames == openGames.tail
      (g._1, g._2)
    }
  }
}
