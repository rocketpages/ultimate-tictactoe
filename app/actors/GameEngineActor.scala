package actors

import actors.messages.akka._
import akka.actor._
import akka.util.Timeout
import scala.concurrent.Await
import akka.pattern.ask
import scala.concurrent.duration._

object GameEngineActor {
  def props = Props(new GameEngineActor)
}

class GameEngineActor extends Actor {
  def receive = {
    case r: RegisterPlayerRequest => handleNewPlayer(r)
  }

  private def handleNewPlayer(r: RegisterPlayerRequest) {
    System.out.println("finding a game for a new player...")
    findOpenGame(r) match {
      case Some(game) => {
        game ! StartGameRequest()
      }
      case None => {
        createNewGame(r)
      }
    }
  }

  private def findOpenGame(request: RegisterPlayerRequest): Option[ActorRef] = {
    System.out.println("finding an open game...")
    for (child <- context.children) {
      val response = attemptRegistration(child, request)
      response.status match {
        case RegisterPlayerResponse.STATUS_GAME_FULL => {} // do nothing, keep looking for an empty game
        case RegisterPlayerResponse.STATUS_OK => {
          response.playerLetter match {
            case Some(letter) => {
              return Some(response.game)
            }
          }
        }
      }
    }
    None
  }

  private def createNewGame(request: RegisterPlayerRequest): ActorRef = {
      val gameUuid = java.util.UUID.randomUUID.toString
      val newGame = context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
      attemptRegistration(newGame, request).game
  }

  private def attemptRegistration(game: ActorRef, request: RegisterPlayerRequest): RegisterPlayerResponse = {
    implicit val timeout = Timeout(10 seconds)
    val future = game ? request
    Await.result(future, timeout.duration).asInstanceOf[RegisterPlayerResponse]
  }

}
