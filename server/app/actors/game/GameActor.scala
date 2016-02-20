package actors.game

import model.akka.ActorMessageProtocol.StartGameMessage
import model.akka.ActorMessageProtocol._
import actors.PlayerLetter
import akka.actor._
import shared.MessageKeyConstants
import shared.ServerToClientProtocol.Game

sealed trait State
case object WaitingForFirstPlayer extends State
case object WaitingForSecondPlayer extends State
case object ActiveGame extends State
case object GameOver extends State

sealed trait Data
final case class OnePlayer(val uuid: String, val x: ActorRef) extends Data
final case class ActiveGame(val uuid: String, val turnActor: ActorRef, val x: ActorRef, val o: ActorRef) extends Data
case object Uninitialized extends Data

object GameActor {
  def props = Props(new GameActor)
}

/**
 * Model the game engine as a finite state machine
 */
class GameActor extends FSM[State, Data] {

  startWith(WaitingForFirstPlayer, Uninitialized)

  when(WaitingForFirstPlayer) {
    case Event(req: RegisterPlayerWithGameMessage, Uninitialized) => {
      goto(WaitingForSecondPlayer) using OnePlayer(req.uuid, req.player)
    }
    case Event(u: UpdateSubscribersWithGameStatus, d: OnePlayer) => {
      u.subscribers.foreach(s => {
        s ! Game(d.uuid, None, None)
      })
      stay
    }
  }

  when(WaitingForSecondPlayer) {
    case Event(req: RegisterPlayerWithGameMessage, p: OnePlayer) => {
      goto(ActiveGame) using ActiveGame(req.uuid, context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), req.player, p.x)
    }
    case Event(u: UpdateSubscribersWithGameStatus, d: OnePlayer) => {
      u.subscribers.foreach(s => {
        s ! Game(d.uuid, None, None)
      })
      stay
    }
  }

  when(ActiveGame) {
    case Event(turn: TurnRequest, game: ActiveGame) => {
      stay using game replying {
        game.turnActor ! TurnRequest(turn.playerLetter, turn.game, turn.grid, Some(game.x), Some(game.o))
      }
    }
    case Event(u: UpdateSubscribersWithGameStatus, d: ActiveGame) => {
      u.subscribers.foreach(s => {
        s ! Game(d.uuid, None, None)
      })
      stay
    }
  }

  onTransition {
    case WaitingForSecondPlayer -> ActiveGame =>
      nextStateData match {
        case g: ActiveGame => {
          g.x ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self)
          g.o ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self)
        }
        case _ => log.error(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}")
      }
  }

  whenUnhandled {
    case Event(msg, _) => {
      log.warning("Received unknown event: " + msg)
      stay
    }
  }

  initialize()

}
