package actors

import actors.GameStatus.GameStatus
import actors.messages.akka._
import actors.messages.json._
import akka.actor._

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  var playerX: Option[ActorRef] = None
  var playerO: Option[ActorRef] = None
  var gameStatus: GameStatus = WAITING
  val board = new GameBoard()

  def receive = {
    case msg: RegisterPlayerRequest => sender ! addPlayerToGame(msg)
    case msg: TurnRequest => handleTurnRequest(msg)
    case msg: StartGameRequest => startGame
  }

  private def handleTurnRequest(turnRequestMsg: TurnRequest) {
    val gameStatus = board.processTurn(turnRequestMsg.gridNum.toInt, turnRequestMsg.playerLetter)

    if (gameStatus == GameStatus.WON)
      handleGameWon(turnRequestMsg)
    else if (gameStatus == GameStatus.TIED)
      handleGameTied(turnRequestMsg)
    else
      handleNextTurn(turnRequestMsg)
  }

  private def handleNextTurn(turnRequestMsg: TurnRequest) {
    val opponent = if (turnRequestMsg.playerLetter == PlayerLetter.O) playerX.get else playerO.get
    opponent ! OpponentTurnResponse(gridId = turnRequestMsg.gridNum, status = OpponentTurnResponse.MESSAGE_YOUR_TURN)
  }

  private def handleGameWon(turnRequestMsg: TurnRequest) {
    handleGameOutcome(false, turnRequestMsg)
  }

  private def handleGameTied(turnRequestMsg: TurnRequest) {
    handleGameOutcome(true, turnRequestMsg)
  }

  private def handleGameOutcome(tied: Boolean, turnRequestMsg: TurnRequest) {
    val playerLetter = turnRequestMsg.playerLetter.toString
    val gameOverResponse = GameOverResponse(tied, playerLetter, turnRequestMsg.gridNum)
    playerX.get ! gameOverResponse
    playerO.get ! gameOverResponse
  }

  private def addPlayerToGame(requestMsg: RegisterPlayerRequest) = {
    System.out.println("adding player to game...")
    val player = requestMsg.player
    getPlayerLetter(player) match {
      case Some(playerLetter) => RegisterPlayerResponse(RegisterPlayerResponse.STATUS_OK, self, Some(playerLetter))
      case _ => RegisterPlayerResponse(RegisterPlayerResponse.STATUS_GAME_FULL, self)
    }
  }

  /**
   * If room exists in this game, assign them to the game and return their letter (X or O)
   * If no room exists in the game, return None instead of a PlayerLetter
   */
  private def getPlayerLetter(player: ActorRef): Option[PlayerLetter.PlayerLetter] = {
    if (playerX == None) {
      playerX = Some(player)
      Some(PlayerLetter.X)
    } else if (playerO == None) {
      playerO = Some(player)
      Some(PlayerLetter.O)
    } else {
      None
    }
  }

  /**
   * If the game has two players registered, start the game and send a message to both players
   */
  private def startGame {
    System.out.println("attempting to start game...")
    if (playerX != None && playerO != None && gameStatus == WAITING) {
      playerX.get ! StartGameResponse(turnIndicator = GameStartResponse.YOUR_TURN, playerLetter = PlayerLetter.X, self)
      playerO.get ! StartGameResponse(turnIndicator = GameStartResponse.WAITING, playerLetter = PlayerLetter.O, self)
    } else {
      throw new RuntimeException("Attempted to start game without 2 players")
    }
  }

}

/**
 * This class encapsulates the state of a game board.
 */
class GameBoard {

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
