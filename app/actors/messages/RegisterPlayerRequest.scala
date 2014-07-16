package actors.messages

import akka.actor.ActorRef

case class RegisterPlayerRequest(player: ActorRef, uuid: String)
