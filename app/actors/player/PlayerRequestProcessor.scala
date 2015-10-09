package actors.player

import akka.actor.{Actor, Props, ActorRef}
import model.akka.{RegisterPlayerRequest, TurnRequest}
import model.json.PlayerRequest

object PlayerRequestProcessorActor {
  def props(gameEngineActor: ActorRef) = Props(new PlayerRequestProcessorActor(gameEngineActor))
}

class PlayerRequestProcessorActor(gameEngineActor: ActorRef) extends Actor {

  def receive = {
    case req: PlayerRequest => {
      val messageType: String = (req.json \ "messageType").as[String]
      System.out.println(req.json)
      if (messageType == "REGISTER_GAME_REQUEST") handleRegisterRequest(req.player)
      else if (messageType == "TURN") handleTurnRequest(req)
    }
  }

  private def handleTurnRequest(req: PlayerRequest) {
    // 1. ensure the player has a letter assigned
    req.maybePlayerLetter match {
      case Some(playerLetter) => {
        // 2. ensure the player belongs to a game
        req.maybeGame match {
          case Some(game) => {
            val gridStr: String = (req.json \ "gridId").as[String]
            val gameId: String = gridStr.charAt(0).toString
            val gridNum: String = gridStr.charAt(1).toString
            System.out.println(s"Player ${req.maybePlayerLetter.get} - game: ${gameId}, grid: ${gridNum}")
            game ! TurnRequest(playerLetter, gameId, gridNum)
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

  private def handleRegisterRequest(player: ActorRef) {
    gameEngineActor ! RegisterPlayerRequest(player)
  }

}
