package actors

import actors.messages.{ RegisterPlayerResponse, RegisterPlayerRequest }
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

  private def handleNewPlayer(r: RegisterPlayerRequest) =
    if (!hopefullyRegisterForExistingGame(r)) createNewGame(r)

  private def hopefullyRegisterForExistingGame(request: RegisterPlayerRequest): Boolean = {
    var foundGame = false
    for (child <- context.children) {
      val response = attemptRegistration(child, request)
      response.playerLetter match {
        case Some(letter) => {
          request.player ! response
          foundGame = true
        }
        case _ => throw new RuntimeException("Where am I? How did I get here?")
      }
    }
    foundGame
  }

  private def createNewGame(request: RegisterPlayerRequest) = {
      val gameUuid = java.util.UUID.randomUUID.toString
      val newGame = context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
      val response = attemptRegistration(newGame, request)
      request.player ! response
  }

  private def attemptRegistration(game: ActorRef, request: RegisterPlayerRequest): RegisterPlayerResponse = {
    implicit val timeout = Timeout(5 seconds)
    val future = game ? request
    Await.result(future, timeout.duration).asInstanceOf[RegisterPlayerResponse]
  }

}

