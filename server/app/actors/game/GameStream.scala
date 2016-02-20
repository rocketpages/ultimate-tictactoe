package actors.player

import akka.actor._
import akka.event.Logging
import model.akka.ActorMessageProtocol.RegisterGameStreamSubscriber
import shared.ServerToClientProtocol._
import shared.MessageKeyConstants
import upickle.default._

object GameStream {
  def props(channel: ActorRef, gameEngineActor: ActorRef) = Props(new GameStream(channel, gameEngineActor))
}

class GameStream(channel: ActorRef, gameEngineActor: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  private var scheduler: Cancellable = _

  override def preStart() {
    // register as a subscriber to game updates
    gameEngineActor ! RegisterGameStreamSubscriber

    // send handshake response
    self ! wrapHandshakeResponse(HandshakeResponse(status = MessageKeyConstants.MESSAGE_OK))

    // start keepalive ping/pong to keep the websocket connection open
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 30 seconds,
      receiver = channel,
      message = upickle.default.write(wrapPing(Ping()))
    )
  }

  def receive = {
    case r: ServerToClientWrapper => channel ! upickle.default.write[ServerToClientWrapper](r)
    case x => log.error("Invalid message: " + x + " - " + sender())
  }

}
