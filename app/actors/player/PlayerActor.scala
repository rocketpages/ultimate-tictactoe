package actors.player

import actors.PlayerLetter.PlayerLetter
import model.akka._
import model.json._
import play.api.libs.json.{JsValue, Json}
import akka.actor.{Actor, Props, ActorRef}

object PlayerActor {
  def props(channel: ActorRef, gameEngineActor: ActorRef) = Props(new PlayerActor(channel, gameEngineActor))
}

class PlayerActor(channel: ActorRef, gameEngineActor: ActorRef) extends Actor {
  var maybeGame: Option[ActorRef] = None
  var maybePlayerLetter: Option[PlayerLetter] = None
  val playerRequestProcessorActor = context.actorOf(PlayerRequestProcessorActor.props(gameEngineActor))

  override def preStart() {
    self ! HandshakeResponse(HandshakeResponse.OK)
  }

  def receive = {
    // incoming messages
    case json: JsValue => playerRequestProcessorActor ! PlayerRequest(json, self, maybePlayerLetter, maybeGame)
    // outbound responses
    case tr: StartGameResponse => handleStartGameResponse(tr)
    case or: OpponentTurnResponse => playerResponse(Json.toJson(or))
    case go: GameOverResponse => playerResponse(Json.toJson(go))
    case hsr: HandshakeResponse => playerResponse(Json.toJson(hsr))
    case bwr: BoardWonResponse => playerResponse(Json.toJson(bwr))
    case _ => throw new Exception("unknown message type")
  }

  private def handleStartGameResponse(tr: StartGameResponse) {
    setGameState(Some(tr.game), Some(tr.playerLetter))
    val response = GameStartResponse(turnIndicator = tr.turnIndicator, playerLetter = tr.playerLetter.toString)
    playerResponse(Json.toJson(response))
  }

  private def playerResponse(r: JsValue) {
    channel ! Json.toJson(r)
  }

  /**
   * This method sets up the game state after the player has been successfully registered for a game. A player cannot belong to more than one game at a time.
   * We could encapsulate this and add additional error checking as an enhancement.
   */
  private def setGameState(mg: Option[ActorRef], mpl: Option[PlayerLetter]) {
    this.maybeGame = mg
    this.maybePlayerLetter = mpl
  }

}
