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
              td(p("")),
              td(
                a(href := "/game/" + "Doug/" + pl.uuid)(p("Join game"))
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
              td(p(oName)),
              td()
            ).render

          jQuery("#game-" + pl.uuid).remove()
          jQuery("#gameList").append(elem)
        }
      }
    }
  }

}