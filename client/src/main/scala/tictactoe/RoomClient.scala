package tictactoe

import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, WebSocket}
import shared.MessageKeyConstants
import shared.ServerToClientProtocol._
import upickle.default._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

import scalatags.Text.all._

import org.scalajs.jquery.jQuery

@JSExportAll
object RoomClient extends js.JSApp {

  // WebSocket connection
  var ws: Option[WebSocket] = None

  def main(): Unit = {}

  def start(): Unit = {
    val WEBSOCKET_URL = "ws://" + dom.document.location.host + "/gamestream"
    ws = Some(new WebSocket(WEBSOCKET_URL))

    // Process turn message ("push") from the server.
    ws.get.onmessage = { (e: MessageEvent) =>
      val data = e.data.toString
      dom.console.log(data)

      val wrapper: ServerToClientWrapper = upickle.default.read[ServerToClientWrapper](data)
      val payload: String = upickle.default.write(wrapper.p)

      wrapper.t.toString match {
        case MessageKeyConstants.MESSAGE_HANDSHAKE => {}
        case MessageKeyConstants.MESSAGE_NEW_GAME_CREATED_EVENT => handleGameCreated(payload)
        case MessageKeyConstants.MESSAGE_GAME_STARTED_EVENT => handleGameStarted(payload)
        case MessageKeyConstants.MESSAGE_GAME_OVER => handleGameOver(payload)
        case MessageKeyConstants.MESSAGE_GAME_REGISTRY_EVENT => handleGameRegistry(payload)
        case "ping" => dom.console.info("pong")
        case _ => dom.console.error("unmatched message from the server", wrapper.t)
      }
    }
  }

  private def handleGameCreated(payload: String): Unit = {
    val pl = read[GameCreatedEvent](payload)
    val xName: String = pl.x

    jQuery("#gameList").show()
    jQuery("#gameListHeader").show()
    jQuery("#gameList").append(openGameRow(pl.uuid, xName))
  }

  private def handleGameStarted(payload: String): Unit = {
    val pl = read[GameStartedEvent](payload)
    val xName: String = pl.x
    val oName: String = pl.o

    jQuery("#gameList").show()
    jQuery("#gameListHeader").show()
    jQuery("#game-" + pl.uuid).remove()
    jQuery("#gameList").append(closedGameRow(pl.uuid, xName, oName))
  }

  private def handleGameOver(payload: String): Unit = {
    val pl = read[GameOverEvent](payload)
    jQuery("#game-" + pl.uuid).remove()
  }

  private def handleGameRegistry(payload: String): Unit = {
    val pl = read[GameRegistryEvent](payload)
    pl.openGames.foreach(g => jQuery("#gameList").append(openGameRow(g.uuid, g.x)))
    pl.closedGames.foreach(g => jQuery("#gameList").append(closedGameRow(g.uuid, g.x, g.o)))
    if (pl.openGames.length > 0 || pl.closedGames.length > 0) {
      jQuery("#gameList").show()
      jQuery("#gameListHeader").show()
    }
  }

  private def openGameRow(uuid: String, xName: String) = {
    tr(id:="game-" + uuid,
      td(p(xName)),
      td(`colspan`:="4", raw("<form action=\"/game/join\" method=\"POST\" class=\"uk-form\">" +
        "<fieldset data-uk-margin>" +
        "<input type=\"text\" name=\"nameO\" id=\"nameO\" placeholder=\"Your name\">" +
        "<input type=\"hidden\" name=\"nameX\" value=\"" + xName + "\">" +
        "<input type=\"hidden\" name=\"uuid\" value=\"" + uuid + "\">" +
        "<button class=\"uk-button\">Join game</button>" +
        "</fieldset>" +
        "</form>")
      )
    ).render
  }

  private def closedGameRow(uuid: String, xName: String, oName: String) = {
    tr(id:="game-" + uuid,
      td(raw(s"<div>${xName} <span>${0} wins</span></div>")),
      td(raw(s"<div>${oName} <span>${0} wins</span></div>")),
      td(div("0")),
      td(div("76")),
      td(div("12:03"))
    ).render
  }

}