package tictactoe

import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, WebSocket}
import shared.MessageKeyConstants
import shared.ServerToClientProtocol.{GameStartedEvent, GameCreatedEvent, ServerToClientWrapper}
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
        case MessageKeyConstants.MESSAGE_NEW_GAME_CREATED_EVENT => {
          val pl = read[GameCreatedEvent](payload)
          val xName: String = pl.x

          val elem =
            tr(id:="game-" + pl.uuid,
              td(p(xName)),
              td(raw("<form action=\"/game/join\" method=\"POST\" class=\"uk-form\">" +
                  "<fieldset data-uk-margin>" +
                    "<input type=\"text\" name=\"nameO\" id=\"nameO\" placeholder=\"Your name\">" +
                    "<input type=\"hidden\" name=\"nameX\" value=\"" + xName + "\">" +
                    "<input type=\"hidden\" name=\"uuid\" value=\"" + pl.uuid + "\">" +
                    "<button class=\"uk-button\">Join game!</button>" +
                  "</fieldset>" +
                "</form>")
              )
            ).render

          jQuery("#gameList").append(elem)
        }
        case MessageKeyConstants.MESSAGE_GAME_STARTED_EVENT => {
          val pl = read[GameStartedEvent](payload)
          val xName: String = pl.x
          val oName: String = pl.o

          val elem =
            tr(id:="game-" + pl.uuid,
              td(p(xName)),
              td(p(oName))
            ).render

          jQuery("#game-" + pl.uuid).remove()
          jQuery("#gameList").append(elem)
        }
      }
    }
  }

}