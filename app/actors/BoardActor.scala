package actors

import actors.ActorMessages.CellSelection
import actors.GameStatus.GameStatus
import akka.actor._

object BoardActor {
  def props() = Props(new BoardActor())
}

class BoardActor() extends Actor {

  import actors.PlayerLetter._

  /* The number of winning combinations are small, so we'll keep it simple and do "brute force" matching.
   * For a game with a larger grid (such as Go), we would need to develop an algorithm, potentially based on
   * "Magic square". http://en.wikipedia.org/wiki/Magic_square
   */
  val WINNING = Array(
    Array(1,2,3),
    Array(4,5,6),
    Array(7,8,9),
    Array(1,4,7),
    Array(2,5,8),
    Array(3,6,9),
    Array(1,5,9),
    Array(3,5,7)
  )

  /*
	 * Represents a flattened game board for Tic Tac Toe. Below is the index value for each game cell.
	 *
	 *    1 | 2 | 3
	 *    4 | 5 | 6
	 *    7 | 8 | 9
	 */
  var cells: Array[Option[PlayerLetter]] = new Array[Option[PlayerLetter]](9)

  /*
   * Request: turn info, cell # and player letter
   * Reply: IN_PROGRESS, WINNER, TIED
   */
  def receive = {
    case msg: CellSelection =>
      sender() ! processTurn(msg.cellNum, msg.player)
  }

  private def processTurn(cellNum: Int, player: PlayerLetter): GameStatus = {
    // 1. mark cell
    cells(cellNum-1) = Some(player)
    // 2. return status of game
    if (isWinner(player)) {
      GameStatus.WON
    }
    else if (isTied) {
      GameStatus.TIED
    }
    else {
      GameStatus.IN_PROGRESS
    }
  }

  /**
   * Compare the current state of the game board with the possible winning combinations to determine a win condition.
   * This should be checked at the end of each turn.
   */
  private def isWinner(player: PlayerLetter): Boolean = {
    var foundWinningCombo = false
    for(index <- 0 until WINNING.length - 1) {
      val possibleCombo = WINNING(index)
      if (cells(possibleCombo(0)-1) == Some(player) && cells(possibleCombo(1)-1) == Some(player) && cells(possibleCombo(2)-1) == Some(player)) {
        foundWinningCombo = true
      }
    }
    foundWinningCombo
  }

  /**
   * Determines if the game is tied. The game is considered tied if there is no winner and all cells have been selected.
   */
  private def isTied = {
    var boardFull = true
    var tied = false
    for(i <- 0 until 9) {
      val letter: Option[PlayerLetter] = cells(i)
      if (letter == None) {
        boardFull = false
      }
    }
    if (boardFull && (!isWinner(PlayerLetter.X) || !isWinner(PlayerLetter.O))) {
      tied = true
    }
    tied
  }

}

