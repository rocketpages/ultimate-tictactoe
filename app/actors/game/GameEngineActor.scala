package actors.game

import akka.event.Logging
import model.akka._
import akka.actor._

import scala.collection.mutable.Queue

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  val log = Logging(context.system, this)

  var openGameQ = new Queue[ActorRef]

  def receive = {
    case r: RegisterPlayerRequest => processRegisterForGameRequest(r)
    case _ => log.error("Invalid message type")
  }

  private def processRegisterForGameRequest(r: RegisterPlayerRequest) {
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
    emitRegisterForGameRequest(game, r)
  }

  private def createNewGame(request: RegisterPlayerRequest): ActorRef = {
    val gameUuid = java.util.UUID.randomUUID.toString
    context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
  }

  private def emitRegisterForGameRequest(game: ActorRef, request: RegisterPlayerRequest) = game ! request
}
