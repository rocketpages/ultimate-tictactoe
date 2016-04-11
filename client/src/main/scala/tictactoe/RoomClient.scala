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
        case MessageKeyConstants.MESSAGE_GAME_STREAM_WON_EVENT => handleGameStreamWonEvent(payload)
        case MessageKeyConstants.MESSAGE_OPEN_GAME_STREAM_UPDATE_EVENT => handleOpenGameStreamUpdateEvent(payload)
        case MessageKeyConstants.MESSAGE_CLOSED_GAME_STREAM_UPDATE_EVENT => handleClosedGameStreamUpdateEvent(payload)
        case MessageKeyConstants.MESSAGE_GAME_STREAM_TURN_EVENT => handleGameStreamTurnEvent(payload)
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
    jQuery("#gameList").append(closedGameRow(pl.uuid, xName, oName, 0, 0, 0, 0))
  }

  private def handleGameOver(payload: String): Unit = {
    val pl = read[GameOverEvent](payload)
    jQuery("#game-" + pl.uuid).remove()

    val gamesInProgress = jQuery("[id^=game-").length

    if (gamesInProgress == 0) {
      jQuery("#gameList").hide()
      jQuery("#gameListHeader").hide()
    }
  }

  private def handleOpenGameStreamUpdateEvent(payload: String): Unit = {
    val pl = read[OpenGameStreamUpdateEvent](payload)
    jQuery("#game-" + pl.uuid).remove()
    jQuery("#gameList").append(openGameRow(pl.uuid, pl.xName))
    jQuery("#gameList").show()
    jQuery("#gameListHeader").show()
  }
  private def handleClosedGameStreamUpdateEvent(payload: String): Unit = {
    val pl = read[ClosedGameStreamUpdateEvent](payload)
    jQuery("#game-" + pl.uuid).remove()
    jQuery("#gameList").append(closedGameRow(pl.uuid, pl.xName, pl.oName, pl.xWins, pl.oWins, pl.totalMoves, pl.totalGames))
    jQuery("#gameList").show()
    jQuery("#gameListHeader").show()
  }

  private def openGameRow(uuid: String, xName: String) = {
    tr(id:="game-" + uuid,
      td(`class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", xName)),
      td(`colspan`:="3", `class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", raw(
        "<form action=\"/game/join\" method=\"POST\" class=\"uk-form\">" +
        "<fieldset data-uk-margin>" +
        "<input type=\"text\" name=\"nameO\" id=\"nameO\" placeholder=\"Your name\">" +
        "<input type=\"hidden\" name=\"nameX\" value=\"" + xName + "\">" +
        "<input type=\"hidden\" name=\"uuid\" value=\"" + uuid + "\">" +
        "<button class=\"uk-button\">Join game</button>" +
        "</fieldset>" +
        "</form>")
      ))
    ).render
  }

  private def closedGameRow(uuid: String, xName: String, oName: String, xWins: Int, oWins: Int, totalMoves: Int, totalGames: Int) = {
    tr(id:="game-" + uuid,
      td(`class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", div(s" ${xName}", span(id:="game-" + uuid + "-xWins", `class`:="uk-badge uk-badge-notification uk-text-small uk-margin-small-left", s"${xWins} wins")))),
      td(`class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", div(s" ${oName}", span(id:="game-" + uuid + "-oWins", `class`:="uk-badge uk-badge-notification uk-text-small uk-margin-small-left", s"${oWins} wins")))),
      td(`class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", div(id:="game-" + uuid + "-total-games", totalGames))),
      td(`class`:="uk-vertical-align", div(`class`:="uk-vertical-align-middle", div(id:="game-" + uuid + "-total-moves", totalMoves)))
    ).render
  }

  private def handleGameStreamWonEvent(payload: String): Unit = {
    val p = read[GameStreamWonEvent](payload)
    jQuery("#game-" + p.uuid + "-xWins").html(p.winsPlayerX.toString + " wins")
    jQuery("#game-" + p.uuid + "-oWins").html(p.winsPlayerO.toString + " wins")
    jQuery("#game-" + p.uuid + "-total-games").html(p.totalGames.toString)
  }

  private def handleGameStreamTurnEvent(payload: String): Unit = {
    val p = read[GameStreamTurnEvent](payload)
    val totalMoves = p.oTurns + p.xTurns
    jQuery("#game-" + p.uuid + "-total-moves").html(totalMoves.toString)
  }

}