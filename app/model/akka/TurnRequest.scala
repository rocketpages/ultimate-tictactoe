package model.akka

import actors.PlayerLetter.PlayerLetter
import akka.actor.ActorRef

case class TurnRequest(playerLetter: PlayerLetter, gridNum: String, playerX: Option[ActorRef] = None, playerO: Option[ActorRef] = None)
