package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{RegisterPlayerResponse, RegisterPlayerRequest}
import akka.actor._
import backend.messages.HandshakeResponse
import play.api.libs.json.JsValue

object PlayerActor {
  def props(channel: ActorRef, gamesActor: ActorRef) = Props(new PlayerActor(channel, gamesActor))
}

class PlayerActor(channel: ActorRef, gamesActor: ActorRef) extends Actor {

  val UUID = java.util.UUID.randomUUID.toString
  var game: Option[ActorRef] = None
  var playerLetter: Option[PlayerLetter] = None

  override def preStart() {
    gamesActor ! RegisterPlayerRequest(self, UUID)
  }

  def receive = {
    case request: JsValue =>
      channel ! ("I received your request")
    case registerPlayerResponse: RegisterPlayerResponse => {
      game = Some(registerPlayerResponse.game)
      val letter = registerPlayerResponse.playerLetter
      channel ! HandshakeResponse(letter)
    }
  }

}
