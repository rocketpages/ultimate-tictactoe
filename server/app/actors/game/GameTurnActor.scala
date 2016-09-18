package actors.game

import actors.{PlayerLetter, GameStatus}
import actors.GameStatus._
import akka.actor.{ActorRef, Props, Actor}
import akka.event.Logging
import model.akka.ActorMessageProtocol._
import model.akka.GameState
import model.akka.GameState.TurnSelection
import shared.MessageKeyConstants
import shared.ServerToClientProtocol._

object GameTurnActor {
  def props = Props(new GameTurnActor)
}

class GameTurnActor extends Actor {

  import actors.PlayerLetter._

  val log = Logging(context.system, this)

  var gameState = new GameState()

  def receive = {
    case m: ProcessTurnMessage => processThisTurn(m)
    case x => log.error("GameTurnActor: Invalid message type: " + x.toString + " / sender: " + sender.toString)
  }

  private def processThisTurn(m: ProcessTurnMessage) {
    val g = gameState.processPlayerSelection(new TurnSelection(m.game.toInt, m.grid.toInt, m.playerLetter)).map { g =>
      processNextTurn(g, m.playerLetter, m)
    }
  }

  private def processNextTurn(g: GameState, p: PlayerLetter, m: ProcessTurnMessage) {
    val (opponent, player) = if (p == PlayerLetter.O)
      (m.x, m.o)
    else
      (m.o, m.x)

    val winningGames = g.getAllWinningGames
    val isGameWon = g.isGameWon()

    opponent ! wrapOpponentTurnResponse(OpponentTurnResponse(m.game.toInt, m.grid.toInt, m.grid.toInt, isGameWon, winningGames, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, m.xTurns, m.oTurns))

    if (isGameWon) {
      player ! wrapBoardWonResponse(BoardWonResponse(m.game))
      context.parent ! GameWonMessage(m.playerLetter.toString, m.game.toInt, m.grid.toInt)
    }
  }

}
