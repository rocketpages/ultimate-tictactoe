import actors.PlayerLetter
import model.akka.GameState
import model.akka.GameState.TurnSelection
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scalaz.{-\/, \/-}

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

      "ensure incomplete game is not flagged as in winning state" in {
        val s1 = TurnSelection(0, 8, PlayerLetter.X)
        val s2 = TurnSelection(8, 1, PlayerLetter.O)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
        } yield (g2)

        result match {
          case \/-(state) => assert(state.isGameWon() == false)
          case _ => fail("scalaz disjunction should be right not left")
        }
      }

      "prevent player from making two moves in a row" in {
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
        val s2 = TurnSelection(0, 1, PlayerLetter.X)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
        } yield (g2)

        assert(result.isLeft)
      }

      "fail if incorrect game square based on opponent's previous move" in {
        val s1 = TurnSelection(0, 2, PlayerLetter.X)
        val s2 = TurnSelection(0, 1, PlayerLetter.O)

        val result = for {
          g1 <- new GameState().processPlayerSelection(s1)
          g2 <- g1.processPlayerSelection(s2)
        } yield (g2)

        assert(result.isLeft)
      }

      "represent a win scenario for player X" in {
        val gameState = for {
          g1 <- new GameState().processPlayerSelection(TurnSelection(0, 2, PlayerLetter.X))
          g2 <- g1.processPlayerSelection(TurnSelection(2, 0, PlayerLetter.O))
          g3 <- g2.processPlayerSelection(TurnSelection(0, 5, PlayerLetter.X))
          g4 <- g3.processPlayerSelection(TurnSelection(5, 0, PlayerLetter.O))
          g5 <- g4.processPlayerSelection(TurnSelection(0, 8, PlayerLetter.X))
          g6 <- g5.processPlayerSelection(TurnSelection(8, 3, PlayerLetter.O))
          g7 <- g6.processPlayerSelection(TurnSelection(3, 2, PlayerLetter.X))
          g8 <- g7.processPlayerSelection(TurnSelection(2, 3, PlayerLetter.O))
          g9 <- g8.processPlayerSelection(TurnSelection(3, 5, PlayerLetter.X))
          g10 <- g9.processPlayerSelection(TurnSelection(5, 3, PlayerLetter.O))
          g11 <- g10.processPlayerSelection(TurnSelection(3, 8, PlayerLetter.X))
          g12 <- g11.processPlayerSelection(TurnSelection(8, 6, PlayerLetter.O))
          g13 <- g12.processPlayerSelection(TurnSelection(6, 2, PlayerLetter.X))
          g14 <- g13.processPlayerSelection(TurnSelection(2, 6, PlayerLetter.O))
          g15 <- g14.processPlayerSelection(TurnSelection(6, 5, PlayerLetter.X))
          g16 <- g15.processPlayerSelection(TurnSelection(5, 6, PlayerLetter.O))
          g17 <- g16.processPlayerSelection(TurnSelection(6, 8, PlayerLetter.X))
        } yield (g17)

        gameState match {
          case \/-(game) => {
            this.info(game.getAllWinningGamesStr)
            assert(game.isGameWon() && game.isGameWonBy(PlayerLetter.X))
          }
          case -\/(fail) => this.fail(fail.message)
        }
      }

      "board array should be formatted properly for client side when board 0 won by player X" in {
        val gameState = for {
          g1 <- new GameState().processPlayerSelection(TurnSelection(0, 2, PlayerLetter.X))
          g2 <- g1.processPlayerSelection(TurnSelection(2, 0, PlayerLetter.O))
          g3 <- g2.processPlayerSelection(TurnSelection(0, 5, PlayerLetter.X))
          g4 <- g3.processPlayerSelection(TurnSelection(5, 0, PlayerLetter.O))
          g5 <- g4.processPlayerSelection(TurnSelection(0, 8, PlayerLetter.X))
        } yield (g5)

        gameState match {
          // game is not in an error state
          case \/-(game) => {
            this.info(game.getAllWinningGamesStr)
            assert(true)
          }
          case -\/(fail) => this.fail(fail.message)
        }
      }
    }
  }
}

