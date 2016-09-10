package actors.game

import model.akka.ActorMessageProtocol.StartGameMessage
import model.akka.ActorMessageProtocol._
import actors.PlayerLetter
import akka.actor._
import shared.ServerToClientProtocol.{GameLostResponse, GameTiedResponse, GameWonResponse}
import shared.{ServerToClientProtocol, MessageKeyConstants}

// game states
sealed trait State

case object WaitingForFirstPlayerState extends State

case object WaitingForSecondPlayerState extends State

case object ActiveGameState extends State

case object AwaitRematchState extends State

// state transition data
sealed trait Data

final case class OnePlayerData(val playerX: Player) extends Data

final case class ActiveGameData(val gameTurnActor: ActorRef, val playerX: Player, val playerO: Player, totalGames: Int) extends Data

final case class AwaitRematchData(playerX: Player, playerO: Player, rematchPlayerX: Option[Boolean], rematchPlayerO: Option[Boolean], totalGames: Int) extends Data

case object Uninitialized extends Data

// inner class
final case class Player(playerActor: ActorRef, name: String, wins: Int, turns: Int, elapsedTime: Int)

object GameActor {
  def props(gameEngineActor: ActorRef, uuid: String) = Props(new GameActor(gameEngineActor, uuid))

  def incrementTurnCount(playerLetter: String, playerX: Player, playerO: Player): (Player, Player) = {
    playerLetter match {
      case "X" => (playerX.copy(turns = (playerX.turns + 1)), playerO)
      case "O" => (playerX, playerO.copy(turns = (playerO.turns + 1)))
    }
  }

  def incrementWonGamesCount(playerLetter: String, playerX: Player, playerO: Player): (Player, Player) = {
    playerLetter match {
      case "X" => (playerX.copy(wins = (playerX.wins + 1)), playerO)
      case "O" => (playerX, playerO.copy(wins = (playerO.wins + 1)))
    }
  }
}

class GameActor(gameEngine: ActorRef, uuid: String) extends FSM[State, Data] {
  startWith(WaitingForFirstPlayerState, Uninitialized)

  when(WaitingForFirstPlayerState) {
    case Event(req: RegisterPlayerWithGameMessage, Uninitialized) => {
      goto(WaitingForSecondPlayerState) using OnePlayerData(Player(req.player, req.name, 0, 0, 0))
    }
  }

  when(WaitingForSecondPlayerState) {
    case Event(req: RegisterPlayerWithGameMessage, p: OnePlayerData) => {
      goto(ActiveGameState) using ActiveGameData(context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), p.playerX, Player(req.player, req.name, 0, 0, 0), 0)
    }
    case Event(m: GameTerminatedMessage, p: OnePlayerData) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      p.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, p: OnePlayerData) => {
      gameEngine ! OpenGameStreamUpdateMessage(uuid, p.playerX.name)
      stay using p
    }
  }

  when(ActiveGameState) {
    case Event(m: TurnMessage, game: ActiveGameData) => {
      val (x, o) = GameActor.incrementTurnCount(m.playerLetter.toString, game.playerX, game.playerO)
      game.gameTurnActor ! ProcessNextTurnMessage(m.playerLetter, m.game, m.grid, x.playerActor, o.playerActor, x.turns, o.turns)
      gameEngine ! GameStreamTurnUpdateMessage(uuid, x.turns, o.turns)
      val g = game.copy(playerX = x, playerO = o)
      stay using g
    }
    case Event(m: GameWonMessage, game: ActiveGameData) => {
      context.stop(game.gameTurnActor)
      val (x1, o1) = GameActor.incrementTurnCount(m.lastPlayer.toString, game.playerX, game.playerO)
      val (x, o) = GameActor.incrementWonGamesCount(m.lastPlayer.toString, x1, o1)
      val totalGames = game.totalGames + 1
      val gameWonResponse = ServerToClientProtocol.wrapGameWonResponse(GameWonResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))
      val gameLostResponse = ServerToClientProtocol.wrapGameLostResponse(GameLostResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames, x.wins, o.wins))

      val (winner, loser) = m.lastPlayer match {
        case "X" => (x.playerActor, o.playerActor)
        case "O" => (o.playerActor, x.playerActor)
      }

      winner ! gameWonResponse
      loser ! gameLostResponse
      gameEngine ! GameWonSubscriberUpdateMessage(uuid, x.wins, o.wins, totalGames)

      goto(AwaitRematchState) using AwaitRematchData(x, o, None, None, totalGames)
    }
    // PlayerActor sends this
    case Event(m: GameTiedMessage, game: ActiveGameData) => {
      // kill the game turn actor, that game is done!
      context.stop(game.gameTurnActor)

      val (x, o) = GameActor.incrementTurnCount(m.lastPlayer.toString, game.playerX, game.playerO)
      val totalGames = game.totalGames + 1

      val response = ServerToClientProtocol.wrapGameTiedResponse(GameTiedResponse(m.lastPlayer, m.lastGameBoardPlayed, m.lastGridPlayed, totalGames))

      game.playerO.playerActor ! response
      game.playerX.playerActor ! response
      gameEngine ! GameTiedSubscriberUpdateMessage(uuid, totalGames)

      goto(AwaitRematchState) using AwaitRematchData(x, o, None, None, totalGames)
    }
    case Event(m: GameTerminatedMessage, game: ActiveGameData) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      game.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      game.playerO.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, game: ActiveGameData) => {
      val totalMoves = game.playerX.turns + game.playerO.turns
      gameEngine ! ClosedGameStreamUpdateMessage(uuid, game.playerX.name, game.playerO.name, game.playerX.wins, game.playerO.wins, totalMoves, game.totalGames)
      stay using game
    }
  }

  when(AwaitRematchState) {
    case Event(m: PlayAgainMessage, state: AwaitRematchData) => {
      val requestedRematch = m.playAgain

      if (requestedRematch == false) {
        gameEngine ! GameOverMessage(uuid, m.player)
        state.playerX.playerActor ! GameOverMessage(uuid, m.player)
        state.playerO.playerActor ! GameOverMessage(uuid, m.player)
        stop
      } else {
        // determine who requested the rematch
        val (playAgainX, playAgainO) = m.player match {
          case "X" => {
            val x = Some(true)
            val o = state.rematchPlayerO
            (x, o)
          }
          case "O" => {
            val o = Some(true)
            val x = state.rematchPlayerX
            (x, o)
          }
        }

        playAgainX match {
          case Some(rematchO) => playAgainO match {
            case Some(rematchX) => {
              // both player x and player o have responded to the rematch request
              if (rematchX && rematchO) {
                goto(ActiveGameState) using ActiveGameData(context.actorOf(Props[GameTurnActor], name = "gameTurnActor"), state.playerX, state.playerO, state.totalGames)
              } else {
                log.error("Game ended in an invalid state")
                stop
              }
            }
            case _ => stay using AwaitRematchData(state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
          }
          case _ => stay using AwaitRematchData(state.playerX, state.playerO, playAgainX, playAgainO, state.totalGames)
        }
      }
    }
    case Event(m: GameTerminatedMessage, state: AwaitRematchData) => {
      gameEngine ! GameOverMessage(uuid, m.terminatedByPlayer)
      state.playerX.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      state.playerO.playerActor ! GameOverMessage(uuid, m.terminatedByPlayer)
      stop
    }
    case Event(m: SendGameStreamUpdateCommand, state: AwaitRematchData) => {
      val totalMoves = state.playerX.turns + state.playerO.turns
      gameEngine ! ClosedGameStreamUpdateMessage(uuid, state.playerX.name, state.playerO.name, state.playerX.wins, state.playerO.wins, totalMoves, state.totalGames)
      stay using state
    }
  }

  onTransition {
    case WaitingForFirstPlayerState -> WaitingForSecondPlayerState => {
      nextStateData match {
        case p: OnePlayerData => {
          p.playerX.playerActor ! GameCreatedMessage(self, PlayerLetter.X)
        }
        case _ => log.error(s"invalid state match for WaitingForFirstPlayer, stateData ${stateData}")
      }
    }
    case WaitingForSecondPlayerState -> ActiveGameState =>
      nextStateData match {
        case g: ActiveGameData => {
          g.playerX.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self, g.playerX.name, g.playerO.name)
          g.playerO.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self, g.playerX.name, g.playerO.name)
          gameEngine ! GameStreamGameStartedMessage(uuid, g.playerX.name, g.playerO.name)
        }
        case _ => log.error(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}")
      }
    case AwaitRematchState -> ActiveGameState =>
      nextStateData match {
        case g: ActiveGameData => {
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
