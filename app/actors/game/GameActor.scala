package actors.game

import actors.GameStatus.GameStatus
import model.akka._
import model.json._
import actors.{GameStatus, PlayerLetter}
import akka.actor._

object GameActor {
  def props = Props(new GameActor)
}

class GameActor extends Actor {

  import actors.GameStatus._

  var playerX: Option[ActorRef] = None
  var playerO: Option[ActorRef] = None

  var gameStatus: GameStatus = WAITING

  val gameTurnActor = context.actorOf(Props[GameTurnActor], name = "gameTurnActor")

  def receive = {
    case msg: TurnRequest => gameTurnActor ! TurnRequest(msg.playerLetter, msg.gridNum, playerX, playerO)
    case msg: RegisterPlayerRequest => {
      sender ! addPlayerToGame(msg)
      tryToStartGame
    }

  }

  private def addPlayerToGame(requestMsg: RegisterPlayerRequest) = {
    System.out.println("adding player to game...")
    val player = requestMsg.player
    getPlayerLetter(player) match {
      case Some(playerLetter) => RegisterPlayerResponse(RegisterPlayerResponse.STATUS_OK, self, Some(playerLetter))
      case _ => RegisterPlayerResponse(RegisterPlayerResponse.STATUS_GAME_FULL, self)
    }
  }

  /**
   * If room exists in this game, assign them to the game and return their letter (X or O)
   * If no room exists in the game, return None instead of a PlayerLetter
   */
  private def getPlayerLetter(player: ActorRef): Option[PlayerLetter.PlayerLetter] = {
    if (playerX == None) {
      playerX = Some(player)
      Some(PlayerLetter.X)
    } else if (playerO == None) {
      playerO = Some(player)
      Some(PlayerLetter.O)
    } else {
      None
    }
  }

  /**
   * If the game has two players registered, start the game and send a message to both players
   */
  private def tryToStartGame {
    if (playerX != None && playerO != None && gameStatus == WAITING) {
      playerX.get ! StartGameResponse(turnIndicator = GameStartResponse.YOUR_TURN, playerLetter = PlayerLetter.X, self)
      playerO.get ! StartGameResponse(turnIndicator = GameStartResponse.WAITING, playerLetter = PlayerLetter.O, self)
    }
  }

}

