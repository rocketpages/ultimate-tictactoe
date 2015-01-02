package actors.messages.akka

import actors.PlayerLetter.PlayerLetter
import akka.actor.ActorRef

object RegisterPlayerResponse {
  val STATUS_GAME_FULL = "game_full"
  val STATUS_OK = "ok"
}

case class RegisterPlayerResponse(status: String, game: ActorRef, playerLetter: Option[PlayerLetter] = None)
