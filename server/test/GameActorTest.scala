import actors.PlayerLetter
import actors.game._
import actors.player.PlayerActor
import akka.actor.ActorSystem
import akka.testkit._
import model.akka.ActorMessageProtocol.{StartGameMessage, GameCreatedMessage, RegisterPlayerWithGameMessage}
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

          // first player creates game
          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor, "Bob")
          fsm ! firstPlayerReq

          // second player registers for game
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor, "Doug")
          fsm ! secondPlayerReq

          // assert that the game is in the ActiveGameState (game has started)
          assert(fsm.stateName == ActiveGameState)
        }

        "send StartGameMessage to both players after the second player has registered for the game" in {
          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))

          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, firstPlayerActor.ref, "Bob")
          fsm ! firstPlayerReq

          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, secondPlayerActor.ref, "Doug")
          fsm ! secondPlayerReq

          firstPlayerActor.expectMsg(500 millis, GameCreatedMessage(fsm, PlayerLetter.X))
          firstPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, playerLetter = PlayerLetter.X, fsm, "Bob", "Doug"))
          secondPlayerActor.expectMsg(500 millis, StartGameMessage(turnIndicator = MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING, playerLetter = PlayerLetter.O, fsm, "Bob", "Doug"))
        }
      }

      "an active game has started" should {

        "handle a new game turn" in {
          assert(false)
        }

        "handle a game won scenario" in {
          assert(false)
        }

        "handle a game tied scenario" in {
          assert(false)
        }

        "handle a terminated game" in {
          assert(false)
        }

        "send game updates to subscribers" in {
          assert(false)
        }

      }

      "awaiting a rematch between the same players" should {

        "be able to start a new game" in {
          assert(false)
        }

        "be able to terminate the game without another rematch" in {
          assert(false)
        }

      }

    }
  }

