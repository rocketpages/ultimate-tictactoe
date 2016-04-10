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

      for (game <- games) {
        game._2.game ! SendGameStreamUpdateCommand()
      }
    }
    case m: OpenGameStreamUpdateMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapOpenGameStreamUpdateEvent(new OpenGameStreamUpdateEvent(m.uuid, m.xName)))
    }
    case m: ClosedGameStreamUpdateMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapClosedGameStreamUpdateEvent(new ClosedGameStreamUpdateEvent(m.uuid, m.xName, m.oName, m.xWins, m.oWins, m.totalGames)))
    }
    // notify all the game stream subscribers
    case m: GameOverMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameOverEvent(GameOverEvent(m.uuid, m.fromPlayer)))
      games.remove(m.uuid)
    }
    case m: GameWonSubscriberUpdateMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameStreamWonEvent(GameStreamWonEvent(m.uuid, m.winsPlayerX, m.winsPlayerO, m.totalGamesPlayed)))
    }
    case m: GameTiedSubscriberUpdateMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameStreamTiedEvent(GameStreamTiedEvent(m.uuid, m.totalGamesPlayed)))
    }
    case m: GameStreamTurnUpdateMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameStreamTurnEvent(GameStreamTurnEvent(m.uuid, m.xTurns, m.oTurns)))
    }
    case m: GameStreamGameStartedMessage => {
      subscribers.toList.foreach(s => s ! ServerToClientProtocol.wrapGameStartedEvent(new GameStartedEvent(m.uuid, m.xName, m.oName)))
    }
    case x => log.error("Invalid type in receive - ", x)
  }
}
