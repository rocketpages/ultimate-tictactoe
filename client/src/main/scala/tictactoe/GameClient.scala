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
import scala.scalajs.js.timers._

@JSExportAll
object GameClient extends js.JSApp {
  // WebSocket connection
  var ws: Option[WebSocket] = None

  var opponentSeconds: Int = 0
  var yourSeconds: Int = 0
  var yourMoves: Int = 0
  var yourTurn: Boolean = false
  var activeGame: Boolean = false
  var player: String = ""
  var opponent: String = ""
  var uuid: Option[String] = None

  def convertSecondsToMmSs(seconds: Int) = {
    val s = seconds % 60
    val m = (seconds / 60) % 60
    f"$m%02.0f" + ":" + f"$s%02.0f"
  }

  def elaspedTimeUpdate = if (yourTurn && activeGame) {
    yourSeconds = yourSeconds+1
    jQuery("#" + player.toLowerCase + "Time").html(convertSecondsToMmSs(yourSeconds))
  } else if (activeGame) {
    opponentSeconds = opponentSeconds+1
    jQuery("#" + opponent.toLowerCase + "Time").html(convertSecondsToMmSs(opponentSeconds))
  }

  // Send your turn information to the server
  private def sendTurnMessage(gameId: Int, gridId: Int) {
    ws.get.send(write[ClientToServerWrapper](wrapTurnCommand(TurnCommand(gameId, gridId))))
  }

  // Process the handshake response when the page is opened
  private def processHandshakeResponse(response: HandshakeResponse, nameX: String, nameO: String): Unit = {
    if (response.status == MessageKeyConstants.MESSAGE_OK) {
      uuid match {
        case Some(id) => ws.get.send(write[ClientToServerWrapper](wrapJoinGameCommand(JoinGameCommand(id, nameX, nameO))))
        case None => ws.get.send(write[ClientToServerWrapper](wrapCreateGameCommand(CreateGameCommand(nameX))))
      }
    }
  }

  private def processGameBoardWon(response: BoardWonResponse): Unit = {
    jQuery("[id^=tile_" + response.gameId + "]").hide()
    jQuery("#winner_" + response.gameId).html(player)
    jQuery("#winner_" + response.gameId).removeClass("color-" + opponent)
    jQuery("#winner_" + response.gameId).addClass("color-" + player)
    jQuery("#winner_" + response.gameId).show()
  }

  private def shouldEnableAllBoardsForThisTurn(response: OpponentTurnResponse): Boolean = {
    // was the last board played an already won board? if so, we need to enable all boards for this turn
    val lastBoardPlayedWinner = response.boardsWonArr(response.nextGameId)
    if (lastBoardPlayedWinner == "X" || lastBoardPlayedWinner == "O") true else false
  }

  private def processOpponentUpdate(response: OpponentTurnResponse): Unit = {
    println(response)

    // Show their turn info on the game board.
    jQuery("#" + response.gridIdSelector).addClass(opponent)
    jQuery("#" + response.gridIdSelector).html(opponent)

    jQuery("#xMoves").html(response.xTurns.toString)
    jQuery("#oMoves").html(response.oTurns.toString)

    // Switch to your turn.
    response.status match {
      case MessageKeyConstants.MESSAGE_GAME_OVER_YOU_WIN => {
        jQuery("[id^=status-").hide()
        jQuery("#status-you-win").show()
      }
      case MessageKeyConstants.MESSAGE_GAME_OVER_TIED => {
        jQuery("[id^=status-").hide()
        jQuery("#status-tie-game").show()
      }
      case _ => {
        yourTurn = true
        if (response.lastBoardWon == true) {
          jQuery("[id^=tile_" + response.gameId + "]").hide()
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

      jQuery("[id^=status-").hide()
      jQuery("#status-your-turn").show()
    }
  }

  private def setPlayerLetter(l: String) {
    player = l // set player
    if (l == "X") opponent = "O" else opponent = "X" // set opponent
  }

  private def processInitialTurn(response: GameStartResponse): Unit = {
    activeGame = true
    clearGameBoard()
    jQuery("#nameO").removeClass("uk-text-muted")
    jQuery("#play_again").hide()
    jQuery("#winsO").show()
    setPlayerLetter(response.playerLetter)
    jQuery("#nameO").text(response.nameO)
    if (response.turnIndicator == MessageKeyConstants.MESSAGE_TURN_INDICATOR_YOUR_TURN) {
      yourTurn = true
      jQuery("[id^=cell_]").prop("disabled", false)
      jQuery("[id^=status-").hide()
      jQuery("#status-your-turn").show()
    } else if (response.turnIndicator == MessageKeyConstants.MESSAGE_TURN_INDICATOR_WAITING) {
      jQuery("[id^=status-").hide()
      jQuery("#status-thinking").show()
    }
  }

  private def processGameTied(response: GameTiedResponse): Unit = {
    activeGame = false
    jQuery("[id^=status-").hide()
    jQuery("#status-tie-game").show()

    // update the board if you didn't make the last move
    if (response.lastPlayer != player) {
      // add opponents last turn to your board
      jQuery("#" + response.lastGridId).addClass(opponent)
      jQuery("#" + response.lastGridId).html(opponent)
    }

    jQuery("#play_again").show()
  }

  private def processGameWon(response: GameWonResponse): Unit = {
    activeGame = false
    jQuery("[id^=tile_" + response.lastGameId + "]").hide()
    jQuery("#winner_" + response.lastGameId).html(player)
    jQuery("#winner_" + response.lastGameId).addClass("color-" + player)
    jQuery("#winner_" + response.lastGameId).show()
    jQuery("[id^=cell_]").prop("disabled", true)
    jQuery("#winsX").html(response.winsX + " wins")
    jQuery("#winsO").html(response.winsO + " wins")
    jQuery("[id^=status-").hide()
    jQuery("#status-you-win").show()
    jQuery("#play_again").show()
  }

  private def processGameLost(response: GameLostResponse): Unit = {
    activeGame = false
    jQuery("[id^=tile_" + response.lastGameId + "]").hide()
    jQuery("#winner_" + response.lastGameId).html(opponent)
    jQuery("#winner_" + response.lastGameId).addClass("color-" + opponent)
    jQuery("#winner_" + response.lastGameId).show()
    jQuery("[id^=cell_]").prop("disabled", true)
    jQuery("#winsX").html(response.winsX + " wins")
    jQuery("#winsO").html(response.winsO + " wins")
    jQuery("[id^=status-").hide()
    jQuery("#status-you-lose").show()
    jQuery("#play_again").show()
  }

  private def clearGameBoard(): Unit = {
    jQuery("[id^=cell_]").prop("disabled", true)
    jQuery("[id^=cell_]").html("")
    jQuery("[id^=cell_]").removeClass(player)
    jQuery("[id^=cell_]").removeClass(opponent)
    jQuery("[id^=tile_]").show()
    jQuery("[id^=winner_]").hide()
  }

  def main(): Unit = {}

  /**
    * Let's play a game, shall we?
    */
  def start(nameX: String, nameO: String, gameId: String): Unit = {
    jQuery(dom.document).ready { () =>

      setInterval(1000)(elaspedTimeUpdate)

      clearGameBoard()

      dom.console.log("i am alive!")

      if (gameId != "") uuid = Some(gameId)

      val WEBSOCKET_URL = getWsProtocol() + dom.document.location.host + "/websocket"
      ws = Some(new WebSocket(WEBSOCKET_URL))

      jQuery("[id^=winner_]").hide()

      jQuery("#play_again_yes").click({
        (thiz: HTMLElement) => {
          jQuery("#play_again").hide()
          ws.get.send(write[ClientToServerWrapper](wrapPlayAgainCommand(PlayAgainCommand(player, true))))
        }
      }: js.ThisFunction)

      jQuery("#play_again_no").click({
        (thiz: HTMLElement) => {
          jQuery("#play_again").hide()
          ws.get.send(write[ClientToServerWrapper](wrapPlayAgainCommand(PlayAgainCommand(player, false))))
        }
      }: js.ThisFunction)

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
            jQuery("[id^=status-").hide()
            jQuery("#status-thinking").show()
            yourMoves = yourMoves+1
            jQuery("#" + player.toLowerCase + "Moves").html(yourMoves.toString)
          }
        }
      }: js.ThisFunction)

      ws.get.onopen = { (e: dom.Event) =>
        jQuery("[id^=status-").hide()
        jQuery("#status-waiting").show()
      }

      ws.get.onclose = { (e: dom.Event) =>
        jQuery("[id^=status-").hide()
        jQuery("#status-game-over").show()
      }

      // Process turn message ("push") from the server.
      ws.get.onmessage = { (e: MessageEvent) =>
        val data = e.data.toString

        val wrapper: ServerToClientWrapper = upickle.default.read[ServerToClientWrapper](data)
        val payload: String = upickle.default.write(wrapper.p)

        println(payload)

        wrapper.t.toString match {
          case MessageKeyConstants.MESSAGE_HANDSHAKE => processHandshakeResponse(read[HandshakeResponse](payload), nameX, nameO)
          case MessageKeyConstants.MESSAGE_BOARD_WON => processGameBoardWon(read[BoardWonResponse](payload))
          case MessageKeyConstants.MESSAGE_OPPONENT_UPDATE => processOpponentUpdate(read[OpponentTurnResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_STARTED => processInitialTurn(read[GameStartResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_LOST => processGameLost(read[GameLostResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_WON => processGameWon(read[GameWonResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_TIED => processGameTied(read[GameTiedResponse](payload))
          case MessageKeyConstants.MESSAGE_GAME_OVER => processGameOver(read[GameOverEvent](payload))
          case MessageKeyConstants.MESSAGE_KEEPALIVE => {}
          case x => {
            dom.console.log("unknown message type: " + x)
          }
        }
      }
    }
  }

  private def processGameOver(payload: GameOverEvent): Unit = {
    activeGame = false
    jQuery("[id^=cell_]").prop("disabled", true)
    jQuery("#play_again").hide()
    if (payload.fromPlayer == player) {
      jQuery("[id^=status-").hide()
      jQuery("#status-game-over-you-ended").show()
    } else {
      jQuery("[id^=status-").hide()
      jQuery("#status-game-over").show()
    }
  }

  private def getWsProtocol(): String = {
    if (dom.document.location.protocol.toString == "https:") {
      "wss://"
    } else {
      "ws://"
    }
  }
}