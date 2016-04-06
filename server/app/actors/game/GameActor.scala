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
case object AwaitRematch extends State
case object GameOver extends State

// state transition data
sealed trait Data
final case class OnePlayer(val uuid: String, val playerX: Player) extends Data
final case class ActiveGame(val uuid: String, val gameTurnActor: ActorRef, val playerX: Player, val playerO: Player, totalGames: Int) extends Data
final case class AwaitRematch(uuid: String, playerX: Player, playerO: Player, rematchPlayerX: Option[Boolean], rematchPlayerO: Option[Boolean], totalGames: Int) extends Data
final case class GameOver(winsPlayerX: Int, winsPlayerO: Int, totalGamesPlayed: Int) extends Data
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
      goto(ActiveGame) using ActiveGame(req.uuid, context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), p.playerX, Player(req.player, req.name, 0), 0)
    }
  }

  when(ActiveGame) {
    case Event(turn: TurnMessage, game: ActiveGame) => {
      game.gameTurnActor ! TurnMessage(turn.playerLetter, turn.game, turn.grid, Some(game.playerX.playerActor), Some(game.playerO.playerActor))
      stay using game
    }
    // PlayerActor sends this
    case Event(m: GameWonMessage, game: ActiveGame) => {
      context.stop(game.gameTurnActor)

      val (x, o) = {
        m.lastPlayer match {
          case "X" => (game.playerX.copy(wins = game.playerX.wins+1), game.playerO)
          case "O" => (game.playerX, game.playerO.copy(wins = game.playerO.wins+1))
        }
      }

      val totalGames = game.totalGames + 1

      x.playerActor ! ServerToClientProtocol.wrapGameWonResponse(GameWonResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
      o.playerActor ! ServerToClientProtocol.wrapGameLostResponse(GameLostResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))

      goto(AwaitRematch) using AwaitRematch(game.uuid, x, o, None, None, totalGames)
    }
    // PlayerActor sends this
    case Event(m: GameTiedMessage, game: ActiveGame) => {
      // kill the game turn actor, that game is done!
      context.stop(game.gameTurnActor)

      val totalGames = game.totalGames + 1
      val response = ServerToClientProtocol.wrapGameTiedResponse(GameTiedResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames))

      game.playerO.playerActor ! response
      game.playerX.playerActor ! response

      goto(AwaitRematch) using AwaitRematch(game.uuid, game.playerX, game.playerO, None, None, totalGames)
    }
  }

  when(AwaitRematch) {
    case Event(m: PlayAgainMessage, state: AwaitRematch) => {

      System.out.println("received play again from " + m.player)

      val (playAgainX, playAgainO) = m.player match {
        case "X" => {
          val x = Some(m.playAgain)
          val o = state.rematchPlayerO
          (x, o)
        }
        case "O" => {
          val o = Some(m.playAgain)
          val x = state.rematchPlayerX
          (x, o)
        }
      }

      playAgainX match {
        case Some(rematchO) => playAgainO match {
          case Some(rematchX) => {
            // both player x and player o have responded to the rematch request
            if (rematchX && rematchO) {
              System.out.println("both x and o have requested a rematch!")
              goto(ActiveGame) using ActiveGame(state.uuid, context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), state.playerX, state.playerO, state.totalGames)
            }
            else {
              System.out.println("one or both of x or o has request not to play!")
              goto(GameOver) using GameOver(state.playerX.wins, state.playerO.wins, state.totalGames)
            }
          }
          case _ => {
            System.out.println("still waiting for x to send their answer")
            stay using AwaitRematch(state.uuid, state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
          }
        }
        case _ => {
          System.out.println("still waiting for o to send their answer")
          stay using AwaitRematch(state.uuid, state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
        }
      }

    }
  }

  when(GameOver) {
    case Event(m: Any, state: Any) => {
      log.error("fuck!")
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
    case ActiveGame -> AwaitRematch => log.debug("transition from ActiveGame to AwaitRematch!")
    case AwaitRematch -> ActiveGame =>
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
      log.error("Received unknown event: " + msg)
      stay
    }
  }

  initialize()

}
