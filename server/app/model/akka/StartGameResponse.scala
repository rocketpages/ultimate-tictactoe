package model.akka

import actors.PlayerLetter._
import akka.actor.ActorRef

case class StartGameResponse(turnIndicator: String, playerLetter: PlayerLetter, game: ActorRef)
