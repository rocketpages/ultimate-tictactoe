package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{ RegisterPlayerResponse, RegisterPlayerRequest }
import akka.actor._
import backend.messages.{ TurnResponse, HandshakeResponse }
import play.api.libs.json.{ Json, JsValue }

object PlayerActor {
  def props(channel: ActorRef, gamesActor: ActorRef) = Props(new PlayerActor(channel, gamesActor))
}

class PlayerActor(channel: ActorRef, gamesActor: ActorRef) extends Actor {
  var maybeGame: Option[ActorRef] = None
  var maybePlayerLetter: Option[PlayerLetter] = None

  override def preStart() {
    gamesActor ! RegisterPlayerRequest(self)
  }

  def receive = {
    case registerPlayerResponse: RegisterPlayerResponse => {
      registerPlayerResponse.playerLetter match {
        case Some(letter) => {
          maybeGame = Some(registerPlayerResponse.game)
          maybePlayerLetter = registerPlayerResponse.playerLetter
          channel ! Json.toJson(HandshakeResponse(letter))
        }
        case _ => throw new RuntimeException("We shouldn't be sending back a handshake without an X or O!")
      }
    }
    case turnResponse: TurnResponse => {
      channel ! Json.toJson(turnResponse)
    }
    case gridId: String => {
      maybeGame match {
        case Some(game) => {
          // send grid ID + player letter to the game
        }
        case _ => // TODO
      }
    }
  }

}
