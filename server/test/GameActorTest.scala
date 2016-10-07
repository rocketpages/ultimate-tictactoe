import actors.PlayerLetter
import actors.game._
import actors.player.PlayerActor
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit._
import model.akka.ActorMessageProtocol._
import model.akka.GameState
import model.akka.GameState.TurnSelection
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import shared.{ServerToClientProtocol, MessageKeyConstants}
import shared.ServerToClientProtocol._
import scala.concurrent.duration._

  class GameActorTest extends TestKit(ActorSystem("GameActorSpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }

    "A GameActor" when {

      "first created" should {

        "be waiting for first player" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestActorRef(new GameEngineActor)

          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          assert(fsm.stateName == WaitingForFirstPlayerState)
          assert(fsm.stateData == Uninitialized)
        }

        "be able to register the first player and transition to WaitingForSecondPlayer state" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestActorRef(new GameEngineActor)

          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val playerActor = TestActorRef(new PlayerActor(TestProbe().ref, gameEngine))
          val req = RegisterPlayerWithGameMessage(uuid: String, playerActor, "Bob")
          fsm ! req
          assert(fsm.stateName == WaitingForSecondPlayerState)
          assert(fsm.stateData == OnePlayerData(Player(playerActor, "Bob", 0, 0, 0)))
        }

        "send GameCreatedMessage to PlayerActor when a new game is created" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestActorRef(new GameEngineActor)

          val fsm = TestFSMRef(new GameActor(gameEngine, uuid))
          val playerProbe = TestProbe()
          val req = RegisterPlayerWithGameMessage(uuid: String, playerProbe.ref, "Bob")
          fsm ! req
          playerProbe.expectMsg(500 millis, GameCreatedMessage(fsm, PlayerLetter.X))
        }

      }

      "waiting for second player" should {

        "register a second player and transition to ActiveGame state" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestActorRef(new GameEngineActor)

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestActorRef(new GameEngineActor)

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

        "handle first turn from player X" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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

        "handle a rematch" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

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
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

          val firstPlayerActor = TestProbe()
          val secondPlayerActor = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))
          fsm.setState(AwaitRematchState, AwaitRematchData(Player(firstPlayerActor.ref, "Bob", 0, 10, 0), Player(secondPlayerActor.ref, "Bob", 1, 10, 0), None, None, 1))

          fsm ! PlayAgainMessage("O", true)

          assert(fsm.stateName == AwaitRematchState)

          fsm ! PlayAgainMessage("X", true)

          assert(fsm.stateName == ActiveGameState)
        }

        "handle a complete game and properly declare X the winner!" in {
          val uuid = java.util.UUID.randomUUID.toString
          val gameEngine = TestProbe()

          val x = TestProbe()
          val o = TestProbe()

          val fsm = TestFSMRef(new GameActor(gameEngine.ref, uuid))

          val firstPlayerReq = RegisterPlayerWithGameMessage(uuid: String, x.ref, "Bob")
          val secondPlayerReq = RegisterPlayerWithGameMessage(uuid: String, o.ref, "Doug")

          // establish game
          fsm ! firstPlayerReq
          fsm ! secondPlayerReq

          x.expectMsgPF() { case GameCreatedMessage(_, PlayerLetter.X) => () }

          x.expectMsgPF() { case StartGameMessage("YOUR_TURN", PlayerLetter.X, _, "Bob", "Doug") => () }
          o.expectMsgPF() { case StartGameMessage("WAITING", PlayerLetter.O, _, "Bob", "Doug") => () }

          gameEngine.expectMsg(GameStreamGameStartedMessage(uuid, "Bob", "Doug"))

          // send first two turn commands
          fsm ! TurnMessage(PlayerLetter.X, "0", "2")

          gameEngine.expectMsg(GameStreamTurnUpdateMessage(uuid, 1, 0))

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(0, 2, 2, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 1, 0)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "2", "0")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(2, 0, 0, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 1, 1)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "0", "5")

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(0, 5, 5, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 2, 1)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "5", "0")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(5, 0, 0, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 2, 2)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "0", "8")

          x.expectMsg(ServerToClientWrapper("board_won", BoardWonResponse("0")))
          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(0, 8, 8, true, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 3, 2)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "8", "3")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(8, 3, 3, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 3, 3)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "3", "2")

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(3, 2, 2, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 4, 3)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "2", "3")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(2, 3, 3, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 4, 4)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "3", "5")

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(3, 5, 5, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 5, 4)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "5", "3")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(5, 3, 3, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 5, 5)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "3", "8")

          x.expectMsg(ServerToClientWrapper("board_won", BoardWonResponse("3")))
          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(3, 8, 8, true, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 6, 5)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "8", "6")

          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(8, 6, 6, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 6, 6)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "6", "2")

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(6, 2, 2, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 7, 6)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "2", "6")

          o.expectMsg(ServerToClientWrapper("board_won", BoardWonResponse("2")))
          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(2, 6, 6, true, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 7, 7)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "6", "5")

          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(6, 5, 5, false, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 8, 7)) => () }

          fsm ! TurnMessage(PlayerLetter.O, "5", "6")

          o.expectMsg(ServerToClientWrapper("board_won", BoardWonResponse("5")))
          x.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(5, 6, 6, true, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 8, 8)) => () }

          fsm ! TurnMessage(PlayerLetter.X, "6", "8")

          x.expectMsg(ServerToClientWrapper("board_won", BoardWonResponse("6")))
          o.expectMsgPF() { case ServerToClientWrapper(MessageKeyConstants.MESSAGE_OPPONENT_UPDATE, OpponentTurnResponse(6, 8, 8, true, _, MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN, 9, 8)) => () }

          x.expectMsg(ServerToClientWrapper("GAME_WON", GameWonResponse("X", 6, 8, 1, 1, 0)))
          o.expectMsg(ServerToClientWrapper("GAME_LOST", GameLostResponse("X", 6, 8, 1, 1, 0)))
        }

      }

    }
  }

