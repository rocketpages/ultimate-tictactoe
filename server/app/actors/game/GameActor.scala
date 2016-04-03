package actors.game

import model.akka.ActorMessageProtocol.StartGameMessage
import model.akka.ActorMessageProtocol._
import actors.PlayerLetter
import akka.actor._
import shared.MessageKeyConstants

sealed trait State
case object WaitingForFirstPlayer extends State
case object WaitingForSecondPlayer extends State
case object ActiveGame extends State
case object GameOver extends State

sealed trait Data
final case class OnePlayer(val uuid: String, val playerX: Player) extends Data
final case class ActiveGame(val uuid: String, val gameTurnActor: ActorRef, val playerX: Player, val playerO: Player) extends Data
final case class FinishedGame(val uuid: String, val playerX: Player, val playerO: Player) extends Data
final case class Player(playerActor: ActorRef, name: String, wins: Int)
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
      goto(WaitingForSecondPlayer) using OnePlayer(req.uuid, Player(req.player, req.name, 0))
    }
  }

  when(WaitingForSecondPlayer) {
    case Event(req: RegisterPlayerWithGameMessage, p: OnePlayer) => {
      goto(ActiveGame) using ActiveGame(req.uuid, context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), p.playerX, Player(req.player, req.name, 0))
    }
  }

  when(ActiveGame) {
    case Event(turn: TurnRequest, game: ActiveGame) => {
      stay using game replying {
        game.gameTurnActor ! TurnRequest(turn.playerLetter, turn.game, turn.grid, Some(game.playerX.playerActor), Some(game.playerO.playerActor))
      }
    }
    // TODO transition from an active game to a finished game
  }

  onTransition {
    case WaitingForSecondPlayer -> ActiveGame =>
      nextStateData match {
        case g: ActiveGame => {
          g.playerX.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self, g.playerX.name, g.playerO.name)
          g.playerO.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self, g.playerX.name, g.playerO.name)
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
