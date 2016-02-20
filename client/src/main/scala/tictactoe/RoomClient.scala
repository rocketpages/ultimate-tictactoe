package tictactoe

import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, WebSocket}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

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
    }
  }

}