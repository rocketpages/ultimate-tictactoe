package actors.player

import akka.actor.{Actor, Props, ActorRef}
import akka.event.Logging
import model.akka.{RegisterPlayerRequest, TurnRequest}
import model.json.PlayerRequest

object PlayerRequestProcessorActor {
  def props(gameEngineActor: ActorRef) = Props(new PlayerRequestProcessorActor(gameEngineActor))
}

class PlayerRequestProcessorActor(gameEngineActor: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case req: PlayerRequest => {
      val messageType: String = (req.json \ "messageType").as[String]
      if (messageType == "REGISTER_GAME_REQUEST")
        handleRegisterRequest(req.player)
      else if (messageType == "TURN")
        handleTurnRequest(req)
    }
    case x => log.error("PlayerRequestProcessorActor: Invalid message type: " + x.getClass + " / sender: " + sender.toString)
  }

  private def handleTurnRequest(req: PlayerRequest) {
    System.out.println(s"handling turn request: ${req}")
    req.maybePlayerLetter match {
      case Some(playerLetter) => {
        req.maybeGame match {
          case Some(game) => {
            val gridStr: String = (req.json \ "gridId").as[String]
            val gameId: String = gridStr.charAt(5).toString
            val gridNum: String = gridStr.charAt(6).toString
            game ! TurnRequest(playerLetter, gameId, gridNum)
          }
          case _ => {
            log.error("player does not belong to a game")
          }
        }
      }
      case _ => {
        log.error("player does not have a letter assigned")
      }
    }
  }

  private def handleRegisterRequest(player: ActorRef) {
    gameEngineActor ! RegisterPlayerRequest(player)
  }

}
