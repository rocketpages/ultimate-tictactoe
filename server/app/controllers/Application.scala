package controllers

import actors.game.GameEngineActor
import actors.player.{GameStream, PlayerActor}
import akka.actor.Props
import play.api.Play.current
import play.api.mvc._
import play.libs.Akka
import model.forms.Forms._
import play.api.i18n.Messages.Implicits._

object Application extends Controller {

  val gameEngineActor = Akka.system.actorOf(Props[GameEngineActor], name = "gameEngineActor")

  /**
   * Renders the UI
   */
  def index = Action {
    Ok(views.html.index(gameForm))
  }

  /**
    * Renders the game room
    */
  def createGame = Action { implicit request =>
    gameForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.index(formWithErrors)),
      f => Ok(views.html.game(Some(f.nameX), None, None))
    )
  }

  /**
    * Renders the game room
    */
  def joinGame = Action { implicit request =>
    joinGameForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.index(gameForm)),
      f => Ok(views.html.game(Some(f.nameX), Some(f.nameO), Some(f.uuid)))
    )
  }

  /**
   * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
   * the actor that Play should create when it receives the WebSocket connection.
   */
  def websocket = WebSocket.acceptWithActor[String, String] { request => channel =>
    PlayerActor.props(channel, gameEngineActor)
  }

  /**
    * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
    * the actor that Play should create when it receives the WebSocket connection.
    */
  def gamestream = WebSocket.acceptWithActor[String, String] { request => channel =>
    GameStream.props(channel, gameEngineActor)
  }

}
