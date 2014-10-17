package actors

import actors.GameStatus.GameStatus
import actors.messages.{ RegisterPlayerRequest, RegisterPlayerResponse, TurnRequest }
import akka.actor._
import backend.messages.{GameOverResponse, OpponentTurnResponse, GameStartResponse}

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  var playerX: Option[ActorRef] = None
  var playerO: Option[ActorRef] = None

  // Status of the current game
  var gameStatus: GameStatus = WAITING

  val board = new GameBoard()

  def receive = {
    case request: RegisterPlayerRequest => {
      val player = request.player
      sender ! RegisterPlayerResponse(self, getLetterForPlayerHopefully(player))
      hopefullyStartGame
    }
    case turnRequest: TurnRequest => {
      System.out.println("Game: processing turn request")
      val gameStatus = board.processTurn(turnRequest.gridNum.toInt, turnRequest.playerLetter)

      if (gameStatus == GameStatus.WON)
      {
        System.out.println("Game: WON")
        val gameOverResponse = if (turnRequest.playerLetter == PlayerLetter.X) {
          GameOverResponse(false, Some("X"))
        } else {
          GameOverResponse(false, Some("O"))
        }
        playerX.get ! gameOverResponse
        playerO.get ! gameOverResponse
      }
      else if (gameStatus == GameStatus.TIED)
      {
        System.out.println("Game: TIED")
        val gameOverResponse = if (turnRequest.playerLetter == PlayerLetter.X) {
          GameOverResponse(true, Some("X"))
        } else {
          GameOverResponse(true, Some("O"))
        }
        playerX.get ! gameOverResponse
        playerO.get ! gameOverResponse
      }
      else
      {
        System.out.println("Game: sending message to opponent")
        val opponent = if (turnRequest.playerLetter == PlayerLetter.O) playerX.get else playerO.get
        opponent ! OpponentTurnResponse(gridId = turnRequest.gridNum, status = OpponentTurnResponse.MESSAGE_YOUR_TURN)
      }
    }
  }

  /**
   * If room exists in this game, assign them to the game and return their letter (X or O)
   * If no room exists in the game, return None instead of a PlayerLetter
   */
  private def getLetterForPlayerHopefully(player: ActorRef): Option[PlayerLetter.PlayerLetter] = {
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
  private def hopefullyStartGame {
    if (playerX != None && playerO != None) {
      playerX.get ! GameStartResponse(turnIndicator = GameStartResponse.YOUR_TURN)
      playerO.get ! GameStartResponse(turnIndicator = GameStartResponse.WAITING)
    }
  }

}

/**
 * This class encapsulates the state of a game board.
 */
class GameBoard {

  import actors.PlayerLetter._

  /*
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
    Array(3, 5, 7))

  /*
	 * Represents a flattened game board for Tic-Tac-Toe. Below is the index value for each game cell.
	 *
	 * 1 | 2 | 3
	 * 4 | 5 | 6
	 * 7 | 8 | 9
	 */
  var cells = Array.fill(9)(None:Option[PlayerLetter])

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
    // 2. return status of game
    val status = if (isWinner(player)) {
      GameStatus.WON
    } else if (isTied) {
      GameStatus.TIED
    } else {
      GameStatus.IN_PROGRESS
    }
    System.out.println("status: " + status)
    status
  }

  /**
   * Compare the current state of the game board with the possible winning combinations to determine a win condition.
   * This should be checked at the end of each turn.
   */
  private def isWinner(player: PlayerLetter): Boolean = {
    var foundWinningCombo = false
    for (index <- 0 until WINNING.length - 1) {
      val possibleCombo = WINNING(index)
      if (cells(possibleCombo(0) - 1) == Some(player) && cells(possibleCombo(1) - 1) == Some(player) && cells(possibleCombo(2) - 1) == Some(player)) {
        foundWinningCombo = true
      }
    }
    System.out.println("winning combo? " + foundWinningCombo)
    foundWinningCombo
  }

  /**
   * Determines if the game is tied. The game is considered tied if there is no winner and all cells have been selected.
   */
  private def isTied: Boolean = {
    var boardFull = true
    var tied = false

    for (cell <- cells) {
      if (cell == None) boardFull = false
    }

    System.out.println("board full: " + boardFull)
    if (boardFull && (!isWinner(PlayerLetter.X) && !isWinner(PlayerLetter.O))) {
      tied = true
    }

    tied
  }

}
