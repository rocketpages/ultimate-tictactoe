package actors

import actors.PlayerLetter.PlayerLetter
import actors.messages.akka._
import actors.messages.json._
import akka.actor._
import play.api.libs.json.{ Json, JsValue }

object PlayerActor {
  def props(channel: ActorRef, gameEngineActor: ActorRef) = Props(new PlayerActor(channel, gameEngineActor))
}

class PlayerActor(channel: ActorRef, gameEngineActor: ActorRef) extends Actor {
  var maybeGame: Option[ActorRef] = None
  var maybePlayerLetter: Option[PlayerLetter] = None

  override def preStart() {
    self ! HandshakeResponse(HandshakeResponse.OK)
  }

  def receive = {
    case tr: StartGameResponse => handleStartGameResponse(tr)
    case or: OpponentTurnResponse => handleOpponentResponse(or)
    case go: GameOverResponse => handleGameOver(go)
    case hsr: HandshakeResponse => channel ! Json.toJson(hsr)
    case json: JsValue => handleJsonRequest(json)
  }

  /**
   * Can be either a register for game request, or a turn request
   */
  private def handleJsonRequest(json: JsValue) {
    val messageType: String = (json \ "messageType").as[String]

    if (messageType == "REGISTER_GAME_REQUEST")
      handleRegisterRequest(json)
    else if (messageType == "TURN")
      handleTurnRequest(json)

    // TODO else send back error message about invalid request
  }

  private def handleRegisterRequest(json: JsValue) {
    gameEngineActor ! RegisterPlayerRequest(self)
  }

  private def handleTurnRequest(json: JsValue) {
    // 1. ensure the player has a letter assigned
    maybePlayerLetter match {
      case Some(playerLetter) => {
        // 2. ensure the player belongs to a game
        maybeGame match {
          case Some(game) => {
            val gridStr: String = (json \ "gridId").as[String]
            val gridNum = gridStr.startsWith("grid_") match {
              case true => gridStr.substring("grid_".length, gridStr.length)
              case false => throw new IllegalArgumentException
            }
            game ! TurnRequest(playerLetter, gridNum)
          }
          case _ => {
            throw new RuntimeException("player does not belong to a game")
            // TODO player does not belong to a game, invalid turn request (we should log this and restart the game, perhaps?)
          }
        }
      }
      case _ => {
        throw new RuntimeException("player does not have a letter assigned")
        // TODO player does not have a letter assigned, invalid turn request (we should log this and restart the game, perhaps?)
      }
    }
  }

  private def handleStartGameResponse(tr: StartGameResponse) {
    System.out.println("starting game for player: " + tr.playerLetter.toString)
    setGameState(Some(tr.game), Some(tr.playerLetter))
    val response = GameStartResponse(turnIndicator = tr.turnIndicator, playerLetter = tr.playerLetter.toString)
    channel ! Json.toJson(response)
  }

  private def handleOpponentResponse(or: OpponentTurnResponse) {
    System.out.println("Game: sending message to " + maybePlayerLetter.get.toString)
    channel ! Json.toJson(or)
  }

  private def handleGameOver(gameOver: GameOverResponse) = {
      channel ! Json.toJson(gameOver)
  }

  /**
   * This method sets up the game state after the player has been successfully registered for a game. A player cannot belong to more than one game at a time.
   * We could encapsulate this and add additional error checking as an enhancement.
   */
  private def setGameState(mg: Option[ActorRef], mpl: Option[PlayerLetter]) {
    this.maybeGame = mg
    this.maybePlayerLetter = mpl
  }

}
