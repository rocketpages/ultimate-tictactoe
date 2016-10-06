import actors.PlayerLetter
import actors.PlayerLetter.PlayerLetter
import actors.game._
import actors.logging.LoggingActor
import actors.player.{GameStream, PlayerActor}
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit._
import akka.util.Timeout
import model.akka.ActorMessageProtocol._
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import shared.ClientToServerProtocol.{CreateGameCommand, MessageType, ClientToServerWrapper}
import shared.MessageKeyConstants
import upickle.default._
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask

class GameEngineActorTest extends TestKit(ActorSystem("GameEngineActorSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val timeout = Timeout(5 seconds)

  "A GameEngineActor" when {

    "a new game is created" should {

      "add a new game to the game queue" in {
        // create a new player actor
        val webSocketProbe = TestProbe()
        val gameEngine = TestActorRef(new GameEngineActor)
        val x = TestActorRef(new PlayerActor(webSocketProbe.ref, gameEngine))

        // game engine list of games is empty
        assert(gameEngine.underlyingActor.games.size == 0)

        // the player creates a new game with the engine
        gameEngine ! CreateGameMessage(x, "Bob")

        // a new game was created and added to the games list
        assert(gameEngine.underlyingActor.games.size == 1)
      }

      "register a game stream subscriber" in {
        // create a new player actor
        val webSocketProbe = TestProbe()
        val gameEngineProbe = TestProbe()

        // represents a player on the main screen waiting for a game
        // automatically registers as a listener in preStart with the game engine
        TestActorRef(new GameStream(webSocketProbe.ref, gameEngineProbe.ref))

        // 1. when GameStream is created it will register itself with the engine as a subscriber
        gameEngineProbe.expectMsgClass(RegisterGameStreamSubscriberMessage.getClass)
      }

      "send GameCreatedMessage to player X" in {
        // create a new player actor
        val gameEngine = TestActorRef(new GameEngineActor)

        // game engine list of games is empty
        assert(gameEngine.underlyingActor.games.size == 0)

        // the player creates a new game with the engine
        gameEngine ! CreateGameMessage(self.actorRef, "Bob")

        // expectMsgPF allows matching on partial message contents instead of the whole message
        expectMsgPF() {
          // GameActor will send this on transition to waiting for the second player
          case GameCreatedMessage(_, PlayerLetter.X) => ()
        }
      }

    }

    "a second player joins a game" should {

    }

  }
}


