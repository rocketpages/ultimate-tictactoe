package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{TurnRequest, RegisterPlayerResponse, RegisterPlayerRequest}
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
    case turnRequest: JsValue => {
      maybeGame match {
        case Some(game) => {
          val gridStr: String = (turnRequest \ "gridId").as[String]
          val gridNum = gridStr.startsWith("grid_") match {
            case true => gridStr.substring("grid_".length, gridStr.length)
            case false => throw new IllegalArgumentException
          }
          game ! TurnRequest(maybePlayerLetter.get, gridNum)
        }
        case _ => // TODO
      }
    }
  }

}
