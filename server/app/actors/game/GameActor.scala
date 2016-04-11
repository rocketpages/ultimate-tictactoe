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

// state transition data
sealed trait Data

final case class OnePlayer(val playerX: Player) extends Data

final case class ActiveGame(val gameTurnActor: ActorRef, val playerX: Player, val playerO: Player, totalGames: Int) extends Data

final case class AwaitRematch(playerX: Player, playerO: Player, rematchPlayerX: Option[Boolean], rematchPlayerO: Option[Boolean], totalGames: Int) extends Data

case object Uninitialized extends Data

// inner class
final case class Player(playerActor: ActorRef, name: String, wins: Int, turns: Int, elapsedTime: Int)

object GameActor {
  def props(gameEngineActor: ActorRef, uuid: String) = Props(new GameActor(gameEngineActor, uuid))
}

/**
  * Model the game engine as a finite state machine
  */
class GameActor(gameEngine: ActorRef, uuid: String) extends FSM[State, Data] {

  startWith(WaitingForFirstPlayer, Uninitialized)

  when(WaitingForFirstPlayer) {
    case Event(req: RegisterPlayerWithGameMessage, Uninitialized) => {
      goto(WaitingForSecondPlayer) using OnePlayer(Player(req.player, req.name, 0, 0, 0))
    }
    case Event(m: GameTerminatedMessage, p: OnePlayer) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      p.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, p: OnePlayer) => {
      gameEngine ! OpenGameStreamUpdateMessage(uuid, p.playerX.name)
      stay using p
    }
  }

  when(WaitingForSecondPlayer) {
    case Event(req: RegisterPlayerWithGameMessage, p: OnePlayer) => {
      goto(ActiveGame) using ActiveGame(context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), p.playerX, Player(req.player, req.name, 0, 0, 0), 0)
    }
    case Event(m: GameTerminatedMessage, p: OnePlayer) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      p.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, p: OnePlayer) => {
      gameEngine ! OpenGameStreamUpdateMessage(uuid, p.playerX.name)
      stay using p
    }
  }

  when(ActiveGame) {
    case Event(m: TurnMessage, game: ActiveGame) => {
      val (x, o) = {
        m.playerLetter.toString match {
          case "X" => (game.playerX.copy(turns = (game.playerX.turns + 1)), game.playerO)
          case "O" => (game.playerX, game.playerO.copy(turns = (game.playerO.turns + 1)))
        }
      }
      game.gameTurnActor ! ProcessNextTurnMessage(m.playerLetter, m.game, m.grid, x.playerActor, o.playerActor, x.turns, o.turns)
      gameEngine ! GameStreamTurnUpdateMessage(uuid, x.turns, o.turns)
      val g = game.copy(playerX = x, playerO = o)
      stay using g
    }
    // PlayerActor sends this
    case Event(m: GameWonMessage, game: ActiveGame) => {
      context.stop(game.gameTurnActor)

      val (x, o) = {
        m.lastPlayer match {
          case "X" => (game.playerX.copy(wins = game.playerX.wins + 1), game.playerO)
          case "O" => (game.playerX, game.playerO.copy(wins = game.playerO.wins + 1))
        }
      }

      val totalGames = game.totalGames + 1

      m.lastPlayer match {
        case "X" => {
          x.playerActor ! ServerToClientProtocol.wrapGameWonResponse(GameWonResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
          o.playerActor ! ServerToClientProtocol.wrapGameLostResponse(GameLostResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
        }
        case "O" => {
          o.playerActor ! ServerToClientProtocol.wrapGameWonResponse(GameWonResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
          x.playerActor ! ServerToClientProtocol.wrapGameLostResponse(GameLostResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
        }
      }

      gameEngine ! GameWonSubscriberUpdateMessage(uuid, x.wins, o.wins, totalGames)
      goto(AwaitRematch) using AwaitRematch(x, o, None, None, totalGames)
    }
    // PlayerActor sends this
    case Event(m: GameTiedMessage, game: ActiveGame) => {
      // kill the game turn actor, that game is done!
      context.stop(game.gameTurnActor)

      val totalGames = game.totalGames + 1
      val response = ServerToClientProtocol.wrapGameTiedResponse(GameTiedResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames))

      game.playerO.playerActor ! response
      game.playerX.playerActor ! response

      gameEngine ! GameTiedSubscriberUpdateMessage(uuid, totalGames)
      goto(AwaitRematch) using AwaitRematch(game.playerX, game.playerO, None, None, totalGames)
    }
    case Event(m: GameTerminatedMessage, game: ActiveGame) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      game.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      game.playerO.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, game: ActiveGame) => {
      val totalMoves = game.playerX.turns + game.playerO.turns
      gameEngine ! ClosedGameStreamUpdateMessage(uuid, game.playerX.name, game.playerO.name, game.playerX.wins, game.playerO.wins, totalMoves, game.totalGames)
      stay using game
    }
  }

  when(AwaitRematch) {
    case Event(m: PlayAgainMessage, state: AwaitRematch) => {
      if (m.playAgain == false) {
        gameEngine ! GameOverMessage(uuid, m.player)
        state.playerX.playerActor ! GameOverMessage(uuid, m.player)
        state.playerO.playerActor ! GameOverMessage(uuid, m.player)
        stop
      } else {
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
                goto(ActiveGame) using ActiveGame(context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), state.playerX, state.playerO, state.totalGames)
              } else {
                log.error("Game ended in an invalid state")
                stop
              }
            }
            case _ => stay using AwaitRematch(state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
          }
          case _ => stay using AwaitRematch(state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
        }
      }
    }
    case Event(m: GameTerminatedMessage, state: AwaitRematch) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      state.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      state.playerO.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, state: AwaitRematch) => {
      val totalMoves = state.playerX.turns + state.playerO.turns
      gameEngine ! ClosedGameStreamUpdateMessage(uuid, state.playerX.name, state.playerO.name, state.playerX.wins, state.playerO.wins, totalMoves, state.totalGames)
      stay using state
    }
  }

  onTransition {
    case WaitingForFirstPlayer -> WaitingForSecondPlayer => {
      nextStateData match {
        case p: OnePlayer => {
          p.playerX.playerActor ! GameCreatedMessage(self, PlayerLetter.X)
        }
        case _ => log.error(s"invalid state match for WaitingForFirstPlayer, stateData ${stateData}")
      }
    }
    case WaitingForSecondPlayer -> ActiveGame =>
      nextStateData match {
        case g: ActiveGame => {
          g.playerX.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self, g.playerX.name, g.playerO.name)
          g.playerO.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self, g.playerX.name, g.playerO.name)
          gameEngine ! GameStreamGameStartedMessage(uuid, g.playerX.name, g.playerO.name)
        }
        case _ => log.error(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}")
      }
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
    case _ => {
      log.error("invalid message received")
      stay
    }
  }

  initialize()

}
