package actors.logging

import akka.actor._
import akka.event.LoggingReceive

class LoggingActor(fac: => Actor) extends Actor {
  val underlying = context.system.actorOf(Props(fac))
  def receive = {
    LoggingReceive {
      case x => {
        System.out.println("underlying: " + underlying.path.toString)
        System.out.println("sender: " + sender.path.toString)
        System.out.println("messageType: " + x.getClass.toString)
        System.out.println("message: " + x.toString)
        underlying.tell(x, sender)
      }
    }
  }
}
