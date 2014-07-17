package actors

import actors.messages.{RegisterPlayerResponse, RegisterPlayerRequest}
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
    case registerPlayerRequest: RegisterPlayerRequest => {
      val player = registerPlayerRequest.player

      var foundEmptyGame = false
      // find an existing game looking for an opponent
      for (child <- context.children) {
        val response = attemptRegistration(child, registerPlayerRequest)
        response.playerLetter match {
          case Some(letter) => {
            player ! response
            foundEmptyGame = true
          }
        }
      }

      // if not found, create a new game and register the player
      val game = if (!foundEmptyGame) {
        val gameUuid = java.util.UUID.randomUUID.toString
        val newGame = context.actorOf(Props[GameActor], name = "gameActor" + gameUuid)
        val response = attemptRegistration(newGame, registerPlayerRequest)
        player ! response
      }
    }
  }

  private def attemptRegistration(game: ActorRef, request: RegisterPlayerRequest): RegisterPlayerResponse = {
    implicit val timeout = Timeout(5 seconds)
    val future = game ? request
    Await.result(future, timeout.duration).asInstanceOf[RegisterPlayerResponse]
  }

}

