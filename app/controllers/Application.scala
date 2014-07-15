package controllers

import actors.PlayerActor
import play.api.Play.current
import play.api.mvc._

object Application extends Controller {

  /**
   * Renders the UI
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
   * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
   * the actor that Play should create when it receives the WebSocket connection.
   */
  def handshake = WebSocket.acceptWithActor[String, String] { request => channel =>
    PlayerActor.props(channel)
  }

}
