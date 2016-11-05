package controllers

import actors.game.GameEngineActor
import actors.player.{GameStream, PlayerActor}
import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import com.google.inject.Inject
import model.forms.Forms._
import play.api.mvc._
import play.api.libs.streams._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class HomeController @Inject() (val messagesApi: MessagesApi)(implicit system: ActorSystem, materializer: Materializer) extends Controller with I18nSupport {

  val gameEngineActor = system.actorOf(Props[GameEngineActor], name = "gameEngineActor")

  /**
   * Renders the UI
   */
  def index = Action.async {
    Future(Ok(views.html.index(gameForm)))
  }

  /**
    * Renders the game room
    */
  def createGame = Action.async { implicit request =>
    Future(
      gameForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.index(formWithErrors)),
        f => Ok(views.html.game(Some(f.nameX), None, None))
      )
    )
  }

  /**
    * Renders the game room
    */
  def joinGame = Action.async { implicit request =>
    Future(
      joinGameForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.index(gameForm)),
        f => Ok(views.html.game(Some(f.nameX), Some(f.nameO), Some(f.uuid)))
      )
    )
  }

  /**
   * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
   * the actor that Play should create when it receives the WebSocket connection.
   */
  def websocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => PlayerActor.props(out, gameEngineActor))
  }

  /**
    * To handle a WebSocket with an actor, we need to give Play a akka.actor.Props object that describes
    * the actor that Play should create when it receives the WebSocket connection.
    */
  def gamestream = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => GameStream.props(out, gameEngineActor))
  }

}
