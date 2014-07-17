package actors.messages

import actors.PlayerLetter.PlayerLetter
import akka.actor.ActorRef

case class RegisterPlayerResponse(game: ActorRef, playerLetter: Option[PlayerLetter])
