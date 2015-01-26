package actors.game

import model.akka.{StartGameRequest, TurnRequest, RegisterPlayerRequest}
import model.json.{GameOverResponse, OpponentTurnResponse}
import actors.{PlayerLetter, GameStatus}
import actors.GameStatus._
import akka.actor.{ActorRef, Props, Actor}

object GameTurnActor {
  def props = Props(new GameTurnActor)
}

class GameTurnActor extends Actor {

  import actors.PlayerLetter._

  /**
   * The number of winning combinations are small, so we'll keep it simple and do "brute force" matching.
   * For a game with a larger grid (such as Go) we'd need to develop an algorithm (potentially based on
   * "Magic square" -- http://en.wikipedia.org/wiki/Magic_square)
   */
  val WINNING = Array(
    Array(1, 2, 3),
    Array(4, 5, 6),
    Array(7, 8, 9),
    Array(1, 4, 7),
    Array(2, 5, 8),
    Array(3, 6, 9),
    Array(1, 5, 9),
    Array(3, 5, 7)
  )

  /**
   * Represents a flattened game board for Tic-Tac-Toe. Below is the index value for each game cell.
   *
   * 1 | 2 | 3
   * 4 | 5 | 6
   * 7 | 8 | 9
   */
  var cells = Array.fill(9)(None: Option[PlayerLetter])

  def receive = {
    case req: TurnRequest => {
      val gameStatus = processTurn(req.gridNum.toInt, req.playerLetter)

      if (gameStatus == GameStatus.WON)
        handleGameWon(req)
      else if (gameStatus == GameStatus.TIED)
        handleGameTied(req)
      else
        handleNextTurn(req)
    }
  }

  private def handleNextTurn(req: TurnRequest) {
    val opponent = if (req.playerLetter == PlayerLetter.O) req.playerX.get else req.playerO.get
    opponent ! OpponentTurnResponse(gridId = req.gridNum, status = OpponentTurnResponse.MESSAGE_YOUR_TURN)
  }

  private def handleGameWon(turnRequestMsg: TurnRequest) {
    handleGameOutcome(false, turnRequestMsg)
  }

  private def handleGameTied(turnRequestMsg: TurnRequest) {
    handleGameOutcome(true, turnRequestMsg)
  }

  private def handleGameOutcome(tied: Boolean, req: TurnRequest) {
    val playerLetter = req.playerLetter.toString
    val gameOverResponse = GameOverResponse(tied, playerLetter, req.gridNum)
    req.playerX.get ! gameOverResponse
    req.playerO.get ! gameOverResponse
  }

  /**
   * Mark the cell the player selected
   */
  def processTurn(cellNum: Int, playerLetter: PlayerLetter): GameStatus = {
    cells(cellNum - 1) = Some(playerLetter)
    getGameStatus(playerLetter)
  }

  /**
   * Return the game status based on the latest turn
   */
  private def getGameStatus(player: PlayerLetter): GameStatus = {
    if (isWinner(player))
      GameStatus.WON
    else if (isTied)
      GameStatus.TIED
    else
      GameStatus.IN_PROGRESS
  }

  /**
   * Compare the current state of the game board with the possible winning combinations to determine a win condition.
   * This should be checked at the end of each turn.
   */
  private def isWinner(player: PlayerLetter): Boolean = {
    var foundWinningCombo = false
    for (index <- 0 until WINNING.length) {
      val possibleCombo = WINNING(index)
      if (cells(possibleCombo(0) - 1) == Some(player) && cells(possibleCombo(1) - 1) == Some(player) && cells(possibleCombo(2) - 1) == Some(player)) {
        foundWinningCombo = true
      }
    }
    foundWinningCombo
  }

  /**
   * Determines if the game is tied. The game is considered tied if there is no winner and all cells have been selected.
   */
  private def isTied: Boolean = {
    var boardFull = true
    var tied = false

    for (cell <- cells)
      if (cell == None)
        boardFull = false

    if (boardFull && (!isWinner(PlayerLetter.X) && !isWinner(PlayerLetter.O)))
      tied = true

    tied
  }

}
