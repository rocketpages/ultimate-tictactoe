package actors.game

import model.akka._
import akka.actor._

import scala.collection.mutable.Queue

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  var openGameQ = new Queue[ActorRef]

  def receive = {
    case r: RegisterPlayerRequest => registerPlayerForGame(r)
  }

  private def registerPlayerForGame(r: RegisterPlayerRequest) {
    val game = if (openGameQ.isEmpty) {
      // create a new game and add it to the queue
      val game = createNewGame(r)
      openGameQ += game
      game
    } else {
      // pull a game off the queue that is waiting for players
      openGameQ.dequeue
    }

    // register player for the game, will start the game when two players register
    registerForGame(game, r)
  }

  private def createNewGame(request: RegisterPlayerRequest): ActorRef = {
    val gameUuid = java.util.UUID.randomUUID.toString
    context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
  }

  private def registerForGame(game: ActorRef, request: RegisterPlayerRequest) = game ! request
}
