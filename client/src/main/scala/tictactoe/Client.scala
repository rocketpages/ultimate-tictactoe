package tictactoe

import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, WebSocket, HTMLElement}
import org.scalajs.jquery.jQuery
import shared.ServerToClientProtocol._
import shared._

import shared.ClientToServerProtocol._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

import upickle.default._

import scala.util.Try

@JSExportAll
object Client extends js.JSApp {
  // WebSocket connection
  var ws: Option[WebSocket] = None

  var yourTurn: Boolean = false
  var player: String = ""
  var opponent: String = ""

  // Send your turn information to the server
  private def sendTurnMessage(gameId: Int, gridId: Int) {
    ws.get.send(write[ClientToServerWrapper](wrapTurnCommand(TurnCommand(gameId, gridId))))
  }

  // Process the handshake response when the page is opened
  private def processHandshakeResponse(response: HandshakeResponse): Unit = {
    if (response.status == MessageKeyConstants.MESSAGE_OK) {
      ws.get.send(write[ClientToServerWrapper](wrapRegisterGameCommand(RegisterGameCommand())))
    }
  }

  private def processGameBoardWon(response: BoardWonResponse): Unit = {
    jQuery("[id^=tile_" + response.gameId + "]").remove()
    jQuery("#winner_" + response.gameId).html(player)
    jQuery("#winner_" + response.gameId).addClass("color-" + player)
    jQuery("#winner_" + response.gameId).show()
  }

  private def shouldEnableAllBoardsForThisTurn(response: OpponentTurnResponse): Boolean = {
    // was the last board played an already won board? if so, we need to enable all boards for this turn
    val lastBoardPlayedWinner = response.boardsWonArr(response.nextGameId - 1)
    if (lastBoardPlayedWinner == "X" || lastBoardPlayedWinner == "O") true else false
  }

  private def processOpponentUpdate(response: OpponentTurnResponse): Unit = {
    // Show their turn info on the game board.
    jQuery("#" + response.gridIdSelector).addClass(opponent)
    jQuery("#" + response.gridIdSelector).html(opponent)

    // Switch to your turn.
    response.status match {
      case MessageKeyConstants.MESSAGE_GAME_OVER_YOU_WIN => jQuery("#status").text(opponent + " is the winner!")
      case MessageKeyConstants.MESSAGE_GAME_OVER_TIED => jQuery("#status").text(MessageKeyConstants.TIED_STATUS)
      case _ => {
        yourTurn = true
        if (response.lastBoardWon == true) {
          jQuery("[id^=tile_" + response.gameId + "]").remove()
          jQuery("#winner_" + response.gameId).html(opponent)
          jQuery("#winner_" + response.gameId).addClass("color-" + opponent)
          jQuery("#winner_" + response.gameId).show()
        }
      }

        // enable all boards because a "won" board was selected for the last turn
        if (shouldEnableAllBoardsForThisTurn(response))
          jQuery("[id^=cell_]").prop("disabled", false)
        else
          jQuery("[id^=cell_" + response.nextGameId + "]").prop("disabled", false)

        jQuery("#status").text(MessageKeyConstants.YOUR_TURN_STATUS)
    }
  }

  private def setPlayerLetter(l: String) {
    player = l // set player
    if (l == "X") opponent = "O" else opponent = "X" // set opponent
  }

  private def processInitialTurn(response: GameStartResponse): Unit = {
    setPlayerLetter(response.playerLetter)
    if (response.turnIndicator == MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN) {
      yourTurn = true
      jQuery("#status").text(MessageKeyConstants.YOUR_TURN_STATUS)
    } else if (response.turnIndicator == MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING) {
      jQuery("[id^=cell_]").prop("disabled", true)
      jQuery("#status").text(MessageKeyConstants.STRATEGIZING_STATUS)
    }
  }

  private def processGameOver(response: GameOverResponse): Unit = {
    if (response.tied) {
      jQuery("#status").text(MessageKeyConstants.TIED_STATUS)

      // update the board if you didn't make the last move
      if (response.lastMovePlayer != player) {
        // add opponents last turn to your board
        jQuery("#" + response.lastGridId).addClass(opponent)
        jQuery("#" + response.lastGridId).html(opponent)
      }
    } else {
      if (response.lastMovePlayer == player) {
        jQuery("[id^=tile_" + response.lastGameId + "]").remove()
        jQuery("#winner_" + response.lastGameId).html(player)
        jQuery("#winner_" + response.lastGameId).addClass("color-" + player)
        jQuery("#winner_" + response.lastGameId).show()
        jQuery("#status").text(MessageKeyConstants.YOU_WIN_STATUS)
      } else {
        jQuery("[id^=tile_" + response.lastGameId + "]").remove()
        jQuery("#winner_" + response.lastGameId).html(opponent)
        jQuery("#winner_" + response.lastGameId).addClass("color-" + opponent)
        jQuery("#winner_" + response.lastGameId).show()
        jQuery("#status").text(MessageKeyConstants.YOU_LOSE_STATUS)
      }
    }
  }

  def main(): Unit = {
    jQuery(dom.document).ready { () =>

      dom.console.log("i am alive!")

      val WEBSOCKET_URL = "ws://" + dom.document.location.host + "/websocket"
      ws = Some(new WebSocket(WEBSOCKET_URL))

      jQuery("[id^=winner_]").hide()

      jQuery("button").click({
        (thiz: HTMLElement) => {
          // Only process clicks if it's your turn AND the square hasn't already been selected
          dom.console.log(thiz.textContent)

          if (yourTurn == true && (thiz.textContent != "X" && thiz.textContent != "O")) {
            yourTurn = false
            val cellId: String = thiz.id
            val gameId = cellId.substring(5,6).toInt
            val gridId = cellId.substring(6,7).toInt
            sendTurnMessage(gameId, gridId)
            // Add the X or O to the game board and update status.
            jQuery("#" + thiz.id).addClass(player)
            jQuery("#" + thiz.id).html(player)
            // Disable all buttons
            jQuery("[id^=cell_]").prop("disabled", true)
            jQuery("#status").text(MessageKeyConstants.STRATEGIZING_STATUS)
          }
        }
      }: js.ThisFunction)

      ws.get.onopen = { (e: dom.Event) =>
        jQuery("#status").text(MessageKeyConstants.WAITING_STATUS)
      }

      ws.get.onclose = { (e: dom.Event) =>
        jQuery("#status").text(MessageKeyConstants.WEBSOCKET_CLOSED_STATUS)
      }

      // Process turn message ("push") from the server.
      ws.get.onmessage = { (e: MessageEvent) =>
        val data = e.data.toString

        val wrapper: ServerToClientWrapper = upickle.default.read[ServerToClientWrapper](data)

        dom.console.log("wrapper type: " + wrapper.t)
        dom.console.log("wrapper payload: " + wrapper.p)

        val payload: String = upickle.default.write(wrapper.p)

        wrapper.t.toString match {
          case MessageKeyConstants.MESSAGE_HANDSHAKE => processHandshakeResponse(read[HandshakeResponse](payload))
          case MessageKeyConstants.MESSAGE_BOARD_WON => processGameBoardWon(read[BoardWonResponse](payload))
          case MessageKeyConstants.MESSAGE_OPPONENT_UPDATE => processOpponentUpdate(read[OpponentTurnResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_STARTED => processInitialTurn(read[GameStartResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_OVER => processGameOver(read[GameOverResponse](payload))
          case MessageKeyConstants.MESSAGE_KEEPALIVE => {}
          case x => {
            dom.console.log("unknown message type: " + x)
          }
        }
      }
    }
  }
}