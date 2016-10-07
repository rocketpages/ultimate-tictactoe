package actors.game

import actors.PlayerLetter._
import model.akka.ActorMessageProtocol.StartGameMessage
import model.akka.ActorMessageProtocol._
import actors.PlayerLetter
import akka.actor._
import model.akka.GameState
import model.akka.GameState.TurnSelection
import shared.ServerToClientProtocol._
import shared.{ServerToClientProtocol, MessageKeyConstants}

import scalaz.{-\/, \/-}

// game states
sealed trait State
case object WaitingForFirstPlayerState extends State
case object WaitingForSecondPlayerState extends State
case object ActiveGameState extends State
case object AwaitRematchState extends State

// state transition data
sealed trait Data
final case class OnePlayerData(val playerX: Player) extends Data
final case class ActiveGameData(val gameState: GameState, val playerX: Player, val playerO: Player, totalGames: Int) extends Data
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
      goto(ActiveGameState) using ActiveGameData(new GameState(), p.playerX, Player(req.player, req.name, 0, 0, 0), 0)
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
      val s = game.gameState.processPlayerSelection(TurnSelection(m.game.toInt, m.grid.toInt, m.playerLetter))

      s match {
        // game is not in an error state
        case \/-(g) => {

          val (opponent, player) = if (m.playerLetter == PlayerLetter.O)
            (game.playerX.playerActor, game.playerO.playerActor)
          else
            (game.playerO.playerActor, game.playerX.playerActor)

          val lastBoardWon = g.isBoardWonBy(m.game.toInt, m.playerLetter)

          val r = OpponentTurnResponse(m.game.toInt, m.grid.toInt, m.grid.toInt, lastBoardWon, g.getAllWinningGames, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, x.turns, o.turns)

          if (lastBoardWon) {
            player ! wrapBoardWonResponse(BoardWonResponse(m.game))
          }

          opponent ! wrapOpponentTurnResponse(r)
          gameEngine ! GameStreamTurnUpdateMessage(uuid, x.turns, o.turns)

          if (g.isGameWon()) {
            self ! GameWonMessage(m.playerLetter.toString, m.game.toInt, m.grid.toInt)
          }

          stay using ActiveGameData(g, x, o, game.totalGames)
        }
        case -\/(fail) => stop(FSM.Failure(fail))
      }

    }
    case Event(m: GameWonMessage, game: ActiveGameData) => {
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
                goto(ActiveGameState) using ActiveGameData(new GameState(), state.playerX, state.playerO, state.totalGames)
              } else {
                stop(FSM.Failure("invalid rematch state"))
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
        case _ => stop(FSM.Failure(s"invalid state match for WaitingForFirstPlayer, stateData ${stateData}"))
      }
    }
    case WaitingForSecondPlayerState -> ActiveGameState =>
      nextStateData match {
        case g: ActiveGameData => {
          g.playerX.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self, g.playerX.name, g.playerO.name)
          g.playerO.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self, g.playerX.name, g.playerO.name)
          gameEngine ! GameStreamGameStartedMessage(uuid, g.playerX.name, g.playerO.name)
        }
        case _ => stop(FSM.Failure(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}"))
      }
    case AwaitRematchState -> ActiveGameState =>
      nextStateData match {
        case g: ActiveGameData => {
          g.playerX.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, self, g.playerX.name, g.playerO.name)
          g.playerO.playerActor ! StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, self, g.playerX.name, g.playerO.name)
        }
        case _ => stop(FSM.Failure(s"invalid state match for WaitingForSecondPlayer, stateData ${stateData}"))
      }
  }

  whenUnhandled {
    case m => {
      stop(FSM.Failure("invalid message received: " + m))
    }
  }

  initialize()

}
