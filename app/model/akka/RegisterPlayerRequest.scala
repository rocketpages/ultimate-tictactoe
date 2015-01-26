package model.akka

import akka.actor.ActorRef

case class RegisterPlayerRequest(player: ActorRef)
