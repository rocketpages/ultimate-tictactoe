import actors.PlayerLetter
import actors.game._
import actors.player.PlayerActor
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit._
import model.akka.ActorMessageProtocol._
import model.akka.GameState
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import shared.MessageKeyConstants
import scala.concurrent.duration._

  class GameActorTest extends TestKit(ActorSystem("GameActorSpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }

    "A GameActor" when {

      "first created" should {

        val uuid = java.util.UUID.randomUUID.toString
        val gameEngine = TestActorRef(new GameEngineActor)

        "be waiting for first player" in {
          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          assert(fsm.stateName == WaitingForFirstPlayerState)
          assert(fsm.stateData == Uninitialized)
        }

        "be able to register the first player and transition to WaitingForSecondPlayer state" in {
          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val playerActor = TestActorRef(new PlayerActor(TestProbe().ref, gameEngine))
          val req = RegisterPlayerWithGameMessage(uuid: String, playerActor, "Bob")
          fsm ! req
          assert(fsm.stateName == WaitingForSecondPlayerState)
          assert(fsm.stateData == OnePlayerData(Player(playerActor, "Bob", 0, 0, 0)))
        }

        "send GameCreatedMessage to PlayerActor when a new game is created" in {
          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val playerProbe = TestProbe()
          val req = RegisterPlayerWithGameMessage(uuid: String, playerProbe.ref, "Bob")
          fsm ! req
          playerProbe.expectMsg(500 millis, GameCreatedMessage(fsm, PlayerLetter.X))
        }

      }

      "waiting for second player" should {

        val uuid = java.util.UUID.randomUUID.toString
        val gameEngine = TestActorRef(new GameEngineActor)

        "register a second player and transition to ActiveGame state" in {
          // bootstrap test
          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val firstPlayerActor = TestActorRef(new PlayerActor(TestProbe().ref, gameEngine))
          val secondPlayerActor = TestActorRef(new PlayerActor(TestProbe().ref, gameEngine))
          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor, "Bob")
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor, "Doug")

          fsm ! firstPlayerReq
          fsm ! secondPlayerReq

          assert(fsm.stateName == ActiveGameState)
        }

        "send StartGameMessage to both players after the second player has registered for the game" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor.ref, "Bob")
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor.ref, "Doug")

          fsm ! firstPlayerReq
          fsm ! secondPlayerReq

          firstPlayerActor.expectMsg(500 millis, GameCreatedMessage(fsm, PlayerLetter.X))
          firstPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, fsm, "Bob", "Doug"))
          secondPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, fsm, "Bob", "Doug"))
        }
      }

      "an active game has started" should {

        val uuid = java.util.UUID.randomUUID.toString
        val gameEngine = TestProbe()

        "handle first turn from player X" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor.ref, "Bob")
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor.ref, "Doug")

          fsm ! firstPlayerReq
          fsm ! secondPlayerReq

          fsm ! TurnMessage(PlayerLetter.X, "1", "1")

          firstPlayerActor.expectMsg(500 millis, GameCreatedMessage(fsm, PlayerLetter.X))
          firstPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, fsm, "Bob", "Doug"))
          secondPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, fsm, "Bob", "Doug"))

          gameEngine.expectMsg(500 millis, GameStreamGameStartedMessage(uuid, "Bob", "Doug"))
          gameEngine.expectMsg(500 millis, GameStreamTurnUpdateMessage(uuid, 1, 0))
        }

        "handle second turn from player O" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor.ref, "Bob")
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor.ref, "Doug")

          // establish game
          fsm ! firstPlayerReq
          fsm ! secondPlayerReq

          gameEngine.expectMsg(500 millis, GameStreamGameStartedMessage(uuid, "Bob", "Doug"))

          // send first two turn commands
          fsm ! TurnMessage(PlayerLetter.X, "0", "2")

          gameEngine.expectMsg(500 millis, GameStreamTurnUpdateMessage(uuid, 1, 0))

          fsm ! TurnMessage(PlayerLetter.O, "2", "5")

          gameEngine.expectMsg(500 millis, GameStreamTurnUpdateMessage(uuid, 1, 1))
        }

        "handle a won game scenario by player X and await rematch" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val gameTurnActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          fsm.setState(ActiveGameState, ActiveGameData(new GameState(), Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 0, 10, 0), 0))

          fsm ! GameWonMessage("X", 1, 1)

          assert(fsm.stateName == AwaitRematchState)
          assert(fsm.stateData == AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 1, 11, 0), Player(secondPlayerActor.ref, "Doug", 0, 10, 0), None, None, 1))
        }

        "handle a won game scenario by player O and await rematch" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val gameTurnActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          fsm.setState(ActiveGameState, ActiveGameData(new GameState(), Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 0, 10, 0), 0))

          fsm ! GameWonMessage("O", 1, 1)

          assert(fsm.stateName == AwaitRematchState)
          assert(fsm.stateData == AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 1, 11, 0), None, None, 1))
        }

        "handle a tied game scenario and await rematch" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()
          val gameTurnActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          fsm.setState(ActiveGameState, ActiveGameData(new GameState(), Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 0, 10, 0), 0))

          fsm ! GameTiedMessage("O", 1, 1)

          assert(fsm.stateName == AwaitRematchState)
          assert(fsm.stateData == AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 0, 11, 0), None, None, 1))
        }

      }

      "awaiting a rematch between the same players" should {

        val uuid = java.util.UUID.randomUUID.toString
        val gameEngine = TestProbe()

        "handle a rematch" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))
          fsm.setState(AwaitRematchState, AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Doug", 1, 10, 0), None, None, 1))

          fsm ! PlayAgainMessage("X", true)

          assert(fsm.stateName == AwaitRematchState)

          fsm ! PlayAgainMessage("O", true)

          assert(fsm.stateName == ActiveGameState)
        }

        "handle a rematch request in opposite message order" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))
          fsm.setState(AwaitRematchState, AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Bob", 1, 10, 0), None, None, 1))

          fsm ! PlayAgainMessage("O", true)

          assert(fsm.stateName == AwaitRematchState)

          fsm ! PlayAgainMessage("X", true)

          assert(fsm.stateName == ActiveGameState)
        }

      }

    }
  }

