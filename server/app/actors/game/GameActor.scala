package actors.game

import model.akka.ActorMessageProtocol.StartGameMessage
import model.akka.ActorMessageProtocol._
import actors.PlayerLetter
import akka.actor._
import shared.ServerToClientProtocol.{GameLostResponse, GameTiedResponse, GameWonResponse}
import shared.{ServerToClientProtocol, MessageKeyConstants}

// game states
sealed trait State
case object WaitingForFirstPlayer extends State
case object WaitingForSecondPlayer extends State
case object ActiveGame extends State
case object GameOver extends State

// state transition data
sealed trait Data
final case class OnePlayer(val uuid: String, val playerX: Player) extends Data
final case class ActiveGame(val uuid: String, val gameTurnActor: ActorRef, val playerX: Player, val playerO: Player) extends Data
final case class GameOver(playerX: Player, playerO: Player, rematchPlayerX: Boolean, rematchPlayerO: Boolean) extends Data
case object Uninitialized extends Data

// inner class
final case class Player(playerActor: ActorRef, name: String, wins: Int)

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
    case Event(turn: TurnMessage, game: ActiveGame) => {
      game.gameTurnActor ! TurnMessage(turn.playerLetter, turn.game, turn.grid, Some(game.playerX.playerActor), Some(game.playerO.playerActor))
      stay using game
    }
    case Event(m: GameWonMessage, game: ActiveGame) => {
      context.stop(game.gameTurnActor)

      val (winner, loser) = m.lastPlayer match {
        case "X" => {
          (game.playerX, game.playerO)
        }
        case "O" => {
          (game.playerO, game.playerX)
        }
      }

      winner.playerActor ! ServerToClientProtocol.wrapGameWonResponse(GameWonResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed))
      loser.playerActor ! ServerToClientProtocol.wrapGameLostResponse(GameLostResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed))

      goto(GameOver) using GameOver(game.playerX, game.playerO, false, false)
    }
    case Event(m: GameTiedMessage, game: ActiveGame) => {
      context.stop(game.gameTurnActor) // kill the game turn actor, that game is done!
      val response = ServerToClientProtocol.wrapGameTiedResponse(GameTiedResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed))
      game.playerO.playerActor ! response
      game.playerX.playerActor ! response
      goto(GameOver) using GameOver(game.playerX, game.playerO, false, false)
    }
  }

  when(GameOver) {
    case Event(a: Any, state: GameOver) => {
      stay
    }
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
