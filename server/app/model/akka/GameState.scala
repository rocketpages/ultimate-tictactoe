package model.akka

import actors.PlayerLetter
import actors.PlayerLetter._
import model.akka.GameState._
import scalaz.{\/, -\/, \/-}

class GameState(selections: List[TurnSelection]) {

  def this() {
    this(List.empty)
  }

  def processPlayerSelection(selection: TurnSelection): InvalidTurnSelection \/ GameState = {
    for {
      v1 <- isSquareSelectionInRange(selection)
      v2 <- isSquareSelectionEmpty(selection, selections)
      v3 <- isSquarePlayedInValidGame(selection, selections, getAllWinningGames)
      v4 <- isLastPlayerValid(selection, selections)
    } yield {
      new GameState(selections :+ selection)
    }
  }

  def turnCount: Int = selections.size

  def isGameWon(): Boolean = {
    isGameWonBy(PlayerLetter.X) || isGameWonBy(PlayerLetter.O)
  }

  def isGameWonBy(player: PlayerLetter): Boolean = {
    val winningGames = getWinningGamesFor(player)
    checkBoardForWinner(winningGames)
  }

  def getWinningGamesFor(player: PlayerLetter) = {
    val board = getGameBoardArray(selections)
    val winningGames = scala.collection.mutable.Set[Int]()

    for (i <- 0 to 8) {
      if (checkBoardForWinner(convertGameArrayToSetForPlayer(board(i), player))) {
        winningGames add i
      }
    }

    winningGames.toSet
  }

  def isBoardWonBy(board: Int, player: PlayerLetter): Boolean = {
    getWinningGamesFor(player).contains(board)
  }

  def getAllWinningGames = {
    val xWins = getWinningGamesFor(PlayerLetter.X)
    val oWins = getWinningGamesFor(PlayerLetter.O)

    val wins = Array.fill(9)("_")

    for (i <- 0 to 8) {
      if (xWins.contains(i))
        wins(i) = PlayerLetter.X.toString
      else if (oWins.contains(i))
        wins(i) = PlayerLetter.O.toString
    }

    wins
  }

  def getAllWinningGamesStr = {
    getAllWinningGames.foldLeft("")((a,v) => a + v + ", ")
  }
}

object GameState {
  type GameBoardArray = Array[Array[Option[PlayerLetter]]]

  case class InvalidTurnSelection(message: String)
  case class TurnSelection(game: Int, square: Int, player: PlayerLetter)

  private val WINNING = List(
    Set(0, 1, 2),
    Set(3, 4, 5),
    Set(6, 7, 8),
    Set(0, 3, 6),
    Set(1, 4, 7),
    Set(2, 5, 8),
    Set(0, 4, 8),
    Set(2, 4, 6)
  )

  private def convertGameArrayToSetForPlayer(board: Array[Option[PlayerLetter]], player: PlayerLetter): Set[Int] = {
    val selectedSquares = scala.collection.mutable.Set[Int]()
    for (i <- 0 to 8) {
      board(i) match {
        case Some(p) => {
          if (p == player)
            selectedSquares += i
        }
        case None => {} // do nothing
      }
    }
    selectedSquares.toSet
  }

  private def isSquareSelectionInRange(turnSelection: TurnSelection): InvalidTurnSelection \/ Boolean = {
    val validGameSelection = if (turnSelection.game >= 0 && turnSelection.game < 9) true else false
    val validSquareSelection = if (turnSelection.square >= 0 && turnSelection.square < 9) true else false

    if (validGameSelection && validSquareSelection)
      \/-(true)
    else
      -\/(InvalidTurnSelection("square is not in valid selection range"))
  }

  private def isSquareSelectionEmpty(thisSelection: TurnSelection, selections: List[TurnSelection]): InvalidTurnSelection \/ Boolean = {
    if (selections.filter(s => (s.game == thisSelection.game && s.square == thisSelection.square)).isEmpty)
      \/-(true)
    else
      -\/(InvalidTurnSelection("square can not be selected twice"))
  }

  private def isSquarePlayedInValidGame(turnSelection: TurnSelection, selections: List[TurnSelection], winningGames: Array[String]): InvalidTurnSelection \/ Boolean = {
    if (selections.nonEmpty) {
      val lastSelection = selections.last
      val lastSelectedSquarePlayedOnWinningGame = winningGames(lastSelection.square) == "X" || winningGames(lastSelection.square) == "O"

      if (lastSelectedSquarePlayedOnWinningGame) {
        \/-(true)
      } else {
        // find the last square selection
        val lastSquare = selections.last.square
        // this board must be equal to the square of the last selection based on ultimate tic-tac-toe rules
        if (lastSquare == turnSelection.game)
          \/-(true)
        else
          -\/(InvalidTurnSelection(s"violates ultimate tic-tac-toe rules: wrong game board played based on previous selection: played:${turnSelection.game}, expected:${lastSquare}"))
      }
    }
    else \/-(true)
  }

  private def isLastPlayerValid(turnSelection: TurnSelection, selections: List[TurnSelection]): InvalidTurnSelection \/ Boolean = {
    if (selections.nonEmpty) {
      // find the last square selection
      val lastTurn = selections.last
      // this board must be equal to the square of the last selection based on ultimate tic-tac-toe rules
      if (!lastTurn.player.equals(turnSelection.player))
        \/-(true)
      else
        -\/(InvalidTurnSelection("violates ultimate tic-tac-toe rules: same player can't make two moves in a row"))
    }
    else \/-(true)
  }

  private def checkBoardForWinner(board: Set[Int]): Boolean = {
    var won = false
    for (combo <- WINNING) {
      val intersect = board.intersect(combo)
      if (intersect.size == 3) {
        won = true
      }
    }
    won
  }

  private def checkBoardForTie(board: Set[Int]): Boolean = ???

  private def getGameBoardArray(selections: List[TurnSelection]): GameBoardArray = {
    val a = Array(
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

    for (turn <- selections) {
      a(turn.game)(turn.square) = Some(turn.player)
    }

    a
  }
}
