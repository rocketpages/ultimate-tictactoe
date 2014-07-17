package actors

import actors.messages.{ RegisterPlayerResponse, RegisterPlayerRequest }
import akka.actor._
import akka.util.Timeout
import scala.concurrent.Await
import akka.pattern.ask
import scala.concurrent.duration._

object GamesActor {
  def props = Props(new GamesActor)
}

class GamesActor extends Actor {
  def receive = {
    // register for an existing game or create a new one and register
    case request: RegisterPlayerRequest => if (!hopefullyRegisterForExistingGame(request)) createNewGame(request)
  }

  private def hopefullyRegisterForExistingGame(request: RegisterPlayerRequest): Boolean = {
    var foundGame = false
    for (child <- context.children) {
      val response = attemptRegistration(child, request)
      response.playerLetter match {
        case Some(letter) => {
          request.player ! response
          foundGame = true
        }
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

