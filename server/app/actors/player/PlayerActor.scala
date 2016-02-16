package actors.player

import actors.PlayerLetter.PlayerLetter
import model.akka._
import model.json._
import play.api.libs.json.{JsValue, Json}
import akka.actor._
import akka.event.Logging

object PlayerActor {
  def props(channel: ActorRef, gameEngineActor: ActorRef) = Props(new PlayerActor(channel, gameEngineActor))
}

class PlayerActor(channel: ActorRef, gameEngineActor: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  var maybeGame: Option[ActorRef] = None
  var maybePlayerLetter: Option[PlayerLetter] = None
  val playerRequestProcessorActor = context.actorOf(PlayerRequestProcessorActor.props(gameEngineActor))

  private var scheduler: Cancellable = _

  override def preStart() {
    // send handshake response
    self ! HandshakeResponse(HandshakeResponse.OK)

    // start keepalive ping/pong to keep the websocket connection open
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 30 seconds,
      receiver = channel,
      message = Json.toJson("ping")
    )
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
    case x => log.error("PlayerActor: Invalid message type: " + x.toString)
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
