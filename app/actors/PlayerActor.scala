package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{RegisterPlayerResponse, RegisterPlayerRequest}
import akka.actor._
import backend.messages.HandshakeResponse
import play.api.libs.json.{Json, JsValue}

object PlayerActor {
  def props(channel: ActorRef, gamesActor: ActorRef) = Props(new PlayerActor(channel, gamesActor))
}

class PlayerActor(channel: ActorRef, gamesActor: ActorRef) extends Actor {
  var game: Option[ActorRef] = None
  var playerLetter: Option[PlayerLetter] = None

  override def preStart() {
    gamesActor ! RegisterPlayerRequest(self)
  }

  def receive = {
    case registerPlayerResponse: RegisterPlayerResponse => {
      game = Some(registerPlayerResponse.game)
      val letter = registerPlayerResponse.playerLetter
      channel ! Json.toJson(HandshakeResponse(letter))
    }
  }

}
