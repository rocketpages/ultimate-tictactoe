package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.{GameOverPlayerResponse, TurnRequest, RegisterPlayerResponse, RegisterPlayerRequest}
import akka.actor._
import backend.messages.{GameOverResponse, OpponentTurnResponse, GameStartResponse, HandshakeResponse}
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
    case r: RegisterPlayerResponse => handleRegisterResponse(r)
    case tr: GameStartResponse => handleTurnResponse(tr)
    case or: OpponentTurnResponse => handleOpponentResponse(or)
    case tReq: JsValue => handleTurnRequest(tReq)
    case go: GameOverResponse => processGameOver(go)
  }

  private def handleRegisterResponse(r: RegisterPlayerResponse) {
    r.playerLetter match {
      case Some(letter) => {
        maybeGame = Some(r.game)
        maybePlayerLetter = r.playerLetter
        channel ! Json.toJson(HandshakeResponse(letter))
      }
      case _ => throw new RuntimeException("We shouldn't be sending back a handshake without an X or O!")
    }
  }

  private def handleTurnResponse(tr: GameStartResponse) {
    channel ! Json.toJson(tr)
  }

  private def handleOpponentResponse(or: OpponentTurnResponse) {
    System.out.println("Game: sending message to " + maybePlayerLetter.get.toString)
    channel ! Json.toJson(or)
  }

  private def handleTurnRequest(tReq: JsValue) {
    maybeGame match {
      case Some(game) => {
        val gridStr: String = (tReq \ "gridId").as[String]
        val gridNum = gridStr.startsWith("grid_") match {
          case true => gridStr.substring("grid_".length, gridStr.length)
          case false => throw new IllegalArgumentException
        }
        game ! TurnRequest(maybePlayerLetter.get, gridNum)
      }
      case _ => throw new RuntimeException("Where am I? How did I get here?")
    }
  }

  private def processGameOver(gameOver: GameOverResponse) = {
      System.out.println("maybePlayerLetter.get:" + maybePlayerLetter.get)
      System.out.println("gameOver.winningPlayer.get:" + gameOver.winningPlayer.get)
      val winningPlayer = if (gameOver.tied) {
        GameOverPlayerResponse.TIED
      } else if (maybePlayerLetter.get.toString == gameOver.winningPlayer.get) {
        GameOverPlayerResponse.YOU_WIN
      } else {
        GameOverPlayerResponse.YOU_LOSE
      }
      val json = GameOverPlayerResponse(winner = winningPlayer)
      channel ! Json.toJson(json)
  }

}
