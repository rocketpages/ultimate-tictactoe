package actors.game

import akka.event.Logging
import model.akka.ActorMessageProtocol._
import akka.actor._
import shared.ServerToClientProtocol
import shared.ServerToClientProtocol._
import scala.collection.mutable.ListBuffer

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  val log = Logging(context.system, this)

  var games = scala.collection.mutable.HashMap.empty[String, GameRecord]
  var subscribers = new ListBuffer[ActorRef]

  case class GameRecord(uuid: String, game: ActorRef, xName: Option[String], oName: Option[String])

  def receive = {
    // find open game and register player for the game
    case c: JoinGameMessage => {
      // register player with the game
      val g = games(c.uuid)
      g.game ! RegisterPlayerWithGameMessage(c.uuid, c.player, c.name)

      // update the game engine record with the new player
      games.update(c.uuid, GameRecord(c.uuid, g.game, g.xName, Some(c.name)))

      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameStartedEvent(new GameStartedEvent(g.uuid, g.xName.getOrElse(""), g.oName.getOrElse(""))))
    }
    case c: CreateGameMessage => {
      // create the game
      val uuid = java.util.UUID.randomUUID.toString
      val newGame = context.actorOf(GameActor.props(self, uuid), name = "gameActor" + uuid)

      // add game to the game engine
      games += (uuid -> GameRecord(uuid, newGame, Some(c.name), None))

      // register player with the game
      newGame ! RegisterPlayerWithGameMessage(uuid, c.player, c.name)

      // notify subscribers of event
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameCreatedEvent(new GameCreatedEvent(uuid, c.name)))
    }
    // register the sender as a subscriber to game updates
    case RegisterGameStreamSubscriberMessage => {
      subscribers += sender()

      val openGamesRecord = games.foldLeft(Array[OpenGameRecord]())(filterOpenGames)
      val closedGamesRecord = games.foldLeft(Array[ClosedGameRecord]())(filterClosedGames)

      sender() ! ServerToClientProtocol.wrapGameRegistryEvent(GameRegistryEvent(openGamesRecord, closedGamesRecord))
    }
    // notify all the game stream subscribers
    case m: GameOverMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameOverEvent(GameOverEvent(m.uuid, m.fromPlayer)))
      games.remove(m.uuid)
    }
    case x => log.error("Invalid type in receive - ", x)
  }

  private def filterOpenGames(array: Array[OpenGameRecord], game: (String, GameRecord)): Array[OpenGameRecord] = {
    game._2.oName match {
      case None => array :+ OpenGameRecord(game._2.uuid, game._2.xName.get)
      case _ => array
    }
  }

  private def filterClosedGames(array: Array[ClosedGameRecord], game: (String, GameRecord)): Array[ClosedGameRecord] = {
    game._2.oName match {
      case None => array
      case _ => array :+ ClosedGameRecord(game._2.uuid, game._2.xName.get, game._2.oName.get)
    }
  }
}
