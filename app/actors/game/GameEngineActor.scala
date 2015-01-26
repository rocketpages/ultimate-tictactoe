package actors.game

import model.akka._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
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
    System.out.println("creating a new game...")
    val gameUuid = java.util.UUID.randomUUID.toString
    val newGame = context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
    attemptRegistration(newGame, request).game
  }

  private def attemptRegistration(game: ActorRef, request: RegisterPlayerRequest): RegisterPlayerResponse = {
    System.out.println("attempting registration...")
    System.out.println("actor type: " + game.toString())
    implicit val timeout = Timeout(3 seconds)
    val future = game ? request
    Await.result(future, timeout.duration).asInstanceOf[RegisterPlayerResponse]
  }

}
