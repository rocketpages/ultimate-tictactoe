package model.json

import actors.PlayerLetter.PlayerLetter
import akka.actor.ActorRef
import play.api.libs.json.JsValue

case class PlayerRequest(json: JsValue, player: ActorRef, maybePlayerLetter: Option[PlayerLetter], maybeGame: Option[ActorRef])
