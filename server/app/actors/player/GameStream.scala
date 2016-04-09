package actors.player

import akka.actor._
import akka.event.Logging
import model.akka.ActorMessageProtocol.RegisterGameStreamSubscriberMessage
import shared.MessageKeyConstants
import shared.ServerToClientProtocol._
import upickle.default._

/**
  * Actor-managed websocket connection for players who are on the main page. Maintains the output channel
  * for game updates from the GameEngineActor.
  */
object GameStream {
  def props(out: ActorRef, gameEngineActor: ActorRef) = Props(new GameStream(out, gameEngineActor))
}

class GameStream(out: ActorRef, gameEngineActor: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  private var scheduler: Cancellable = _

  override def preStart() {
    // register as a subscriber to game updates
    gameEngineActor ! RegisterGameStreamSubscriberMessage

    // send handshake response
    self ! wrapHandshakeResponse(HandshakeResponse(status = MessageKeyConstants.MESSAGE_OK))

    // start keepalive ping/pong to keep the websocket connection open
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 30 seconds,
      receiver = out,
      message = upickle.default.write(wrapPing(Ping()))
    )
  }

  def receive = {
    case r: ServerToClientWrapper => out ! upickle.default.write[ServerToClientWrapper](r)
    case x => log.error("Invalid message: " + x + " - " + sender())
  }

}
