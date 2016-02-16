package actors.game

import model.akka.{StartGameRequest, TurnRequest, RegisterPlayerRequest}
import model.json.{BoardWonResponse, GameOverResponse, OpponentTurnResponse}
import actors.{PlayerLetter, GameStatus}
import actors.GameStatus._
import akka.actor.{ActorRef, Props, Actor}
import akka.event.Logging

object GameTurnActor {
  def props = Props(new GameTurnActor)
}

class GameTurnActor extends Actor {

  import actors.PlayerLetter._

  val log = Logging(context.system, this)

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
  var games: Array[Array[Option[PlayerLetter]]] =
    Array(
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter]),
      Array.fill(9)(None: Option[PlayerLetter])
    )

  var boardsWon: Array[Option[PlayerLetter]] = Array.fill(9)(None: Option[PlayerLetter])
  var boardsTied: Array[Boolean] = Array.fill(9)(false)
  var validBoardId: Option[Int] = None

  def receive = {
    case req: TurnRequest => processTurnCommand(req)
    case x => log.error("GameTurnActor: Invalid message type: " + x.toString + " / sender: " + sender.toString)
  }

  private def processTurnCommand(req: TurnRequest) {
    System.out.println(s"processing turn command: ${req}")
    // check to make sure the current turn is being played on a valid square
    if (validBoardId.isEmpty || validBoardId.get == req.game.toInt) {
      val gameStatus = processTurn(req.game.toInt, req.grid.toInt, req.playerLetter)
      if (gameStatus == GameStatus.WON)
        handleGameWon(req)
      else if (gameStatus == GameStatus.TIED)
        handleGameTied(req)
      else
        handleNextTurn(req)
    } else {
      log.error("Invalid board being played (not playing a valid square)")
    }
  }

  private def handleNextTurn(req: TurnRequest) {
    val (opponent, player) = if (req.playerLetter == PlayerLetter.O)
      (req.playerX.get, req.playerO.get)
    else
      (req.playerO.get, req.playerX.get)

    val lastBoardWon = boardsWon(req.game.toInt - 1).isDefined

    opponent ! OpponentTurnResponse(gameId = req.game, gridId = "cell_" + req.game + req.grid, nextGameId = req.grid, lastBoardWon = lastBoardWon, allBoardsWon = boardsWonArray, status = OpponentTurnResponse.MESSAGE_YOUR_TURN)

    if (lastBoardWon) {
      player ! BoardWonResponse(gameId = req.game)
    }
  }

  private def handleGameWon(turnRequestMsg: TurnRequest) {
    handleGameOutcome(false, turnRequestMsg)
  }

  private def handleGameTied(turnRequestMsg: TurnRequest) {
    handleGameOutcome(true, turnRequestMsg)
  }

  private def handleGameOutcome(tied: Boolean, req: TurnRequest) {
    val playerLetter = req.playerLetter.toString
    val gameOverResponse = GameOverResponse(tied, playerLetter, req.game + req.grid, req.game)
    req.playerX.get ! gameOverResponse
    req.playerO.get ! gameOverResponse
  }

  /**
   * Mark the cell the player selected
   */
  def processTurn(gameNum: Int, cellNum: Int, playerLetter: PlayerLetter): GameStatus = {
    // 1. mark the cell played with an X or an O
    games(gameNum - 1)(cellNum - 1) = Some(playerLetter)

    // 2. did the player win an inner game board?
    if (isWinner(games(gameNum - 1), playerLetter)) {
      boardsWon(gameNum - 1) = Some(playerLetter)
    } else {
      val lastBoardPlayedTied = isTied(games(gameNum - 1))
      if (lastBoardPlayedTied) boardsTied(gameNum) = true
    }

    getGameStatus(playerLetter)
  }

  /**
   * Return the game status based on the latest turn
   */
  private def getGameStatus(player: PlayerLetter): GameStatus = {
    if (isWinner(boardsWon, player))
      GameStatus.WON
    else if (isTied(boardsWon))
      GameStatus.TIED
    else
      GameStatus.IN_PROGRESS
  }

  /**
   * Compare the current state of the game board with the possible winning combinations to determine a win condition.
   * This should be checked at the end of each turn.
   */
  private def isWinner(gameToCheck: Array[Option[PlayerLetter]], player: PlayerLetter): Boolean = {
    var foundWinningCombo = false
    for (index <- 0 until WINNING.length) {
      val possibleCombo = WINNING(index)
      if (gameToCheck(possibleCombo(0) - 1) == Some(player) && gameToCheck(possibleCombo(1) - 1) == Some(player) && gameToCheck(possibleCombo(2) - 1) == Some(player)) {
        foundWinningCombo = true
      }
    }
    foundWinningCombo
  }

  /**
   * Determines if the game is tied. The game is considered tied if there is no winner and all cells have been selected.
   */
  private def isTied(gameToCheck: Array[Option[PlayerLetter]]): Boolean = {
    var boardFull = true
    var tied = false

    for (cell <- gameToCheck)
      if (cell == None)
        boardFull = false

    if (boardFull && (!isWinner(gameToCheck, PlayerLetter.X) && !isWinner(gameToCheck, PlayerLetter.O)))
      tied = true

    tied
  }

  private def boardsWonArray = boardsWon.map(x => x.getOrElse("").toString)

}
