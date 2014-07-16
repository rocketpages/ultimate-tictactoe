import actors.PlayerLetter.PlayerLetter
import akka.actor.ActorRef

package object actors {

  object GameStatus extends Enumeration {
    type GameStatus = Value
    val WAITING, IN_PROGRESS, WON, TIED = Value
  }

  object PlayerLetter extends Enumeration {
    type PlayerLetter = Value
    val X, O = Value
  }

  object ActorMessages {
    case class CellSelection(player: PlayerLetter, cellNum: Int)
    case class FindGame()
    case class
  }

}
