package actors.player

import actors.PlayerLetter.PlayerLetter
import model.akka.ActorMessageProtocol._
import model.akka._
import akka.actor._
import akka.event.Logging
import shared.ClientToServerProtocol._
import shared.ServerToClientProtocol._
import shared.MessageKeyConstants
import upickle.default._

object PlayerActor {
  def props(out: ActorRef, gameEngineActor: ActorRef) = Props(new PlayerActor(out, gameEngineActor))
}

class PlayerActor(out: ActorRef, gameEngineActor: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  var maybeGame: Option[ActorRef] = None
  var maybePlayerLetter: Option[PlayerLetter] = None

  private var scheduler: Cancellable = _

  override def preStart() {
    // send handshake response
    self ! wrapHandshakeResponse(HandshakeResponse(status = MessageKeyConstants.MESSAGE_OK))

    // start keepalive ping/pong to keep the websocket connection open
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 30 seconds,
      receiver = out,
      message = upickle.default.write(wrapPing(Ping()))
    )
  }

  override def postStop() = {
    maybeGame match {
      case Some(game) => {
        game ! GameTerminatedMessage(maybePlayerLetter.get.toString)
      }
      case None => {
        log.error("player actor killed but no game assigned")
      }
    }
  }

  def receive = {
    case incoming: String => handleIncomingMessage(incoming)
    case tr: StartGameMessage => startGame(tr)
    case g: GameCreatedMessage => setGameState(Some(g.gameActor), Some(g.playerLetter))
    case go: GameOverMessage => out ! upickle.default.write[ServerToClientWrapper](wrapGameOverEvent(GameOverEvent(go.uuid, go.fromPlayer)))
    case r: ServerToClientWrapper => out ! upickle.default.write[ServerToClientWrapper](r)
    case _ => log.error("Invalid message")
  }

  private def startGame(tr: ActorMessageProtocol.StartGameMessage) {
    setGameState(Some(tr.game), Some(tr.playerLetter))
    val r = wrapGameStartResponse(GameStartResponse(turnIndicator = tr.turnIndicator, playerLetter = tr.playerLetter.toString, nameX = tr.nameX, nameO = tr.nameO))
    out ! upickle.default.write[ServerToClientWrapper](r)
  }

  /**
   * This method sets up the game state after the player has been successfully registered for a game. A player cannot belong to more than one game at a time.
   * We could encapsulate this and add additional error checking as an enhancement.
   */
  private def setGameState(mg: Option[ActorRef], mpl: Option[PlayerLetter]) {
    this.maybeGame = mg
    this.maybePlayerLetter = mpl
  }

  private def handleIncomingMessage(s: String): Unit = {
    val wrapper: ClientToServerWrapper = upickle.default.read[ClientToServerWrapper](s)
    val payload: String = upickle.default.write(wrapper.p)

    wrapper.t.toString match {
      case MessageKeyConstants.MESSAGE_JOIN_GAME_COMMAND => {
        val pl = read[JoinGameCommand](payload)
        gameEngineActor ! JoinGameMessage(self, pl.nameO, pl.uuid)
      }
      case MessageKeyConstants.MESSAGE_CREATE_GAME_COMMAND => {
        val pl = read[CreateGameCommand](payload)
        gameEngineActor ! CreateGameMessage(self, pl.nameX)
      }
      case MessageKeyConstants.MESSAGE_TURN_COMMAND => {
        handleTurnRequest(read[TurnCommand](payload))
      }
      case MessageKeyConstants.MESSAGE_PLAY_AGAIN_COMMAND => {
        handlePlayAgain(read[PlayAgainCommand](payload))
      }
      case _ => {
        log.error("Invalid payload type")
      }
    }
  }

  private def handleTurnRequest(c: TurnCommand) {
    maybePlayerLetter match {
      case Some(playerLetter) => maybeGame match {
          case Some(game) => game ! TurnMessage(playerLetter, c.gameId.toString, c.gridId.toString)
          case _ => log.error("player does not belong to a game")
      }
      case _ => log.error("player does not have a letter assigned")
    }
  }

  private def handlePlayAgain(c: PlayAgainCommand): Unit = {
    maybePlayerLetter match {
      case Some(playerLetter) => maybeGame match {
        case Some(game) => {
          game ! PlayAgainMessage(playerLetter.toString, c.playAgain)
        }
        case _ => log.error("No game")
      }
      case None => log.error("No player letter")
    }
  }

}
