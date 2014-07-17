package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{RegisterPlayerResponse, RegisterPlayerRequest}
import akka.actor._
import backend.messages.{TurnResponse, HandshakeResponse}
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
      registerPlayerResponse.playerLetter match {
        case Some(letter) => channel ! Json.toJson(HandshakeResponse(letter))
        case _ => throw new RuntimeException("We shouldn't be sending back a handshake without an X or O!")
      }
    }
    case turnResponse: TurnResponse => {
      channel ! Json.toJson(turnResponse)
    }
  }

}
