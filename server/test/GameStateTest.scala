import actors.PlayerLetter
import model.akka.GameState
import model.akka.GameState.TurnSelection
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class GameStateTest extends WordSpecLike with Matchers with BeforeAndAfterAll {

  "GameState" when {

    "first created" should {

      "be empty" in {
        val gameState = new GameState
        assert(gameState.turnCount == 0)
      }

    }

    "representing an active game" should {

      "successfully process unique turn selections" in {
        val s1 = TurnSelection(0, 8, PlayerLetter.X)
        val s2 = TurnSelection(8, 1, PlayerLetter.O)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
        } yield (g2)

        assert(result.isRight)
      }

      "disallow same player from making two moves in a row" in {
        val s1 = TurnSelection(0, 8, PlayerLetter.X)
        val s2 = TurnSelection(8, 1, PlayerLetter.X)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
        } yield (g2)

        assert(result.isLeft)
      }

      "fail on duplicate turn selections" in {
        val s1 = TurnSelection(0, 0, PlayerLetter.X)
        val s2 = TurnSelection(0, 1, PlayerLetter.O)
        val s3 = TurnSelection(0, 2, PlayerLetter.X)
        val s4 = TurnSelection(0, 2, PlayerLetter.O)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
          g3 <- g2.processPlayerSelection(s3)
          g4 <- g3.processPlayerSelection(s4)
        } yield (g4)

        assert(result.isLeft)
      }

    }

  }

}
