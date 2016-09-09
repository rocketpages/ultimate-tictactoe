import actors.game._
import actors.player.PlayerActor
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit._
import model.akka.ActorMessageProtocol.RegisterPlayerWithGameMessage
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

  class GameActorTest extends TestKit(ActorSystem("GameActorSpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }

    "A GameActor" when {

      "first created" should {

        val uuid = java.util.UUID.randomUUID.toString
        val gameEngine = TestActorRef(new GameEngineActor)
        val fsm = TestFSMRef(new GameActor(gameEngine, uuid))

        val websocketProbe = TestProbe()
        val playerActor = TestActorRef(new PlayerActor(websocketProbe.ref, gameEngine))

        "be waiting for first player" in {
          assert(fsm.stateName == WaitingForFirstPlayer)
          assert(fsm.stateData == Uninitialized)
        }

        "be able to register a second player" in {
          val req = RegisterPlayerWithGameMessage(uuid: String, playerActor, "Bob")
          fsm ! req
          assert(fsm.stateName == WaitingForSecondPlayer)
          assert(fsm.stateData == OnePlayer(Player(playerActor, "Bob", 0, 0, 0)))
        }

      }

    }
  }

