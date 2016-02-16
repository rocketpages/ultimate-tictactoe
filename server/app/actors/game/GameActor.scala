package actors.game

import model.akka._
import model.json._
import actors.PlayerLetter
import akka.actor._

sealed trait State
case object WaitingForFirstPlayer extends State
case object WaitingForSecondPlayer extends State
case object ActiveGame extends State
case object GameOver extends State

sealed trait Data
final case class OnePlayer(val x: ActorRef) extends Data
final case class ActiveGame(val turnActor: ActorRef, val x: ActorRef, val o: ActorRef) extends Data
case object Uninitialized extends Data

object GameActor {
  def props = Props(new GameActor)
}

/**
 * Model the game as a series of state transitions
 */
class GameActor extends FSM[State, Data] {

  startWith(WaitingForFirstPlayer, Uninitialized)

  when(WaitingForFirstPlayer) {
    case Event(req: RegisterPlayerRequest, Uninitialized) => {
      goto(WaitingForSecondPlayer) using OnePlayer(req.player)
    }
  }

  when(WaitingForSecondPlayer) {
    case Event(req: RegisterPlayerRequest, p: OnePlayer) => {
      goto(ActiveGame) using ActiveGame(context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), req.player, p.x)
    }
  }

  when(ActiveGame) {
    case Event(turn: TurnRequest, game: ActiveGame) => {
      stay using game replying {
        game.turnActor ! TurnRequest(turn.playerLetter, turn.game, turn.grid, Some(game.x), Some(game.o))
      }
    }
  }

  onTransition {
    case WaitingForSecondPlayer -> ActiveGame =>
      nextStateData match {
        case g: ActiveGame => {
          g.x ! StartGameResponse(turnIndicator = GameStartResponse.YOUR_TURN, playerLetter = PlayerLetter.X, self)
          g.o ! StartGameResponse(turnIndicator = GameStartResponse.WAITING, playerLetter = PlayerLetter.O, self)
        }
        case _ => log.error(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}")
      }
  }

  whenUnhandled {
    case Event(msg, _) =>
      log.warning("Received unknown event: " + msg)
      stay
  }

  initialize()

}
