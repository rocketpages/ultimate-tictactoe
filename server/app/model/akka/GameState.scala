package model.akka

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
      v3 <- isSquarePlayedInValidGame(selection, selections)
      v4 <- isLastPlayerValid(selection, selections)
    } yield {
      new GameState(selections :+ selection)
    }
  }

  def isSquareSelectionInRange(turnSelection: TurnSelection): InvalidTurnSelection \/ Boolean = {
    val validGameSelection = if (turnSelection.game >= 0 && turnSelection.game < 9) true else false
    val validSquareSelection = if (turnSelection.square >= 0 && turnSelection.square < 9) true else false

    if (validGameSelection && validSquareSelection)
      \/-(true)
    else
      -\/(InvalidTurnSelection("square is not in valid selection range"))
  }

  def isSquareSelectionEmpty(thisSelection: TurnSelection, previousSelections: List[TurnSelection]): InvalidTurnSelection \/ Boolean = {
    if (selections.filter(s => (s.game == thisSelection.game && s.square == thisSelection.square)).isEmpty)
      \/-(true)
    else
      -\/(InvalidTurnSelection("square can not be selected twice"))
  }

  def isSquarePlayedInValidGame(turnSelection: TurnSelection, selections: List[TurnSelection]): InvalidTurnSelection \/ Boolean = {
    if (selections.nonEmpty) {
      // find the last square selection
      val lastSquare = selections.head.square
      // this board must be equal to the square of the last selection based on ultimate tic-tac-toe rules
      if (lastSquare == turnSelection.game)
        \/-(true)
      else
        -\/(InvalidTurnSelection("violates ultimate tic-tac-toe rules: wrong game board played based on previous selection"))
    }
    else \/-(true)
  }

  def isLastPlayerValid(turnSelection: TurnSelection, selections: List[TurnSelection]): InvalidTurnSelection \/ Boolean = {
    if (selections.nonEmpty) {
      // find the last square selection
      val lastTurn = selections.head
      // this board must be equal to the square of the last selection based on ultimate tic-tac-toe rules
      if (!lastTurn.player.equals(turnSelection.player))
        \/-(true)
      else
        -\/(InvalidTurnSelection("violates ultimate tic-tac-toe rules: same player can't make two moves in a row"))
    }
    else \/-(true)
  }

  def turnCount: Int = selections.size

}

object GameState {
  case class InvalidTurnSelection(message: String)
  case class TurnSelection(game: Int, square: Int, player: PlayerLetter)
}
