package controllers

import actors.game.GameEngineActor
import actors.player.PlayerActor
import akka.actor.Props
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.mvc._
import play.libs.Akka

object Application extends Controller {

  val gameEngineActor = Akka.system.actorOf(Props[GameEngineActor], name = "gameEngineActor")

  /**
   * Renders the UI
   */
  def index = Action {
    Ok(views.html.index(""))
  }

  /**
   * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
   * the actor that Play should create when it receives the WebSocket connection.
   */
  def websocket = WebSocket.acceptWithActor[JsValue, JsValue] { request => channel =>
    PlayerActor.props(channel, gameEngineActor)
  }

}
