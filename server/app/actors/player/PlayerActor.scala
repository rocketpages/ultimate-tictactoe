package actors.player

import actors.PlayerLetter.PlayerLetter
import model.akka._
import model.json._
import akka.actor._
import akka.event.Logging
import play.api.libs.json.{Json, JsValue}
import shared.MessageKeyConstants
import shared.ServerToClientMessages._

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
    self ! HandshakeResponse(MessageKeyConstants.MESSAGE_OK)

    // start keepalive ping/pong to keep the websocket connection open
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 30 seconds,
      receiver = channel,
      message = upickle.default.write[String]("ping")
    )
  }

  def receive = {
    // incoming messages
    case json: JsValue => {
      log.debug("incoming message to akka: " + json.toString())
      playerRequestProcessorActor ! PlayerRequest(json, self, maybePlayerLetter, maybeGame)
    }
    // prepare start game response
    case tr: StartGameResponse => handleStartGameResponse(tr)
    // direct messages to player
    case or: OpponentTurnResponse => channel ! Json.toJson(upickle.default.write[OpponentTurnResponse](or))
    case go: GameOverResponse => channel ! Json.toJson(upickle.default.write[GameOverResponse](go))
    case hsr: HandshakeResponse => channel ! Json.toJson(upickle.default.write[HandshakeResponse](hsr))
    case bwr: BoardWonResponse => channel ! Json.toJson(upickle.default.write[BoardWonResponse](bwr))
    case x => log.error("PlayerActor: Invalid message type: " + x.toString)
  }

  private def handleStartGameResponse(tr: StartGameResponse) {
    setGameState(Some(tr.game), Some(tr.playerLetter))
    val response = GameStartResponse(turnIndicator = tr.turnIndicator, playerLetter = tr.playerLetter.toString)
    channel ! Json.toJson(upickle.default.write[GameStartResponse](response))
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
