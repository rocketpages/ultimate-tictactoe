package shared

object MessageKeyConstants {
  // Constants - Status Updates
  val STRATEGIZING_STATUS = "Your opponent is strategizing."
  val WAITING_STATUS = "Waiting for an opponent."
  val YOUR_TURN_STATUS = "It's your turn!"
  val YOU_WIN_STATUS = "You win!"
  val YOU_LOSE_STATUS = "You lose!"
  val TIED_STATUS = "The game is tied."
  val WEBSOCKET_CLOSED_STATUS = "The WebSocket Connection Has Been Closed."

  // Constants - Game
  val PLAYER_O = "O"
  val PLAYER_X = "X"

  // Constants - Incoming message types
  val MESSAGE_HANDSHAKE = "handshake"
  val MESSAGE_REGISTER_GAME_RESPONSE = "register_game_response"
  val MESSAGE_OPPONENT_UPDATE = "response"
  val MESSAGE_GAME_STARTED = "GAME_STARTED"
  val MESSAGE_GAME_OVER = "GAME_OVER"
  val MESSAGE_BOARD_WON = "board_won"
  val MESSAGE_KEEPALIVE = "ping"
  val MESSAGE_OK = "OK"
  val MESSAGE_TURN_COMMAND = "TURN_COMMAND"
  val MESSAGE_JOIN_GAME_COMMAND = "JOIN_GAME_COMMAND"
  val MESSAGE_CREATE_GAME_COMMAND = "CREATE_GAME_COMMAND"
  val MESSAGE_GAME_UPDATE = "CREATE_GAME_UPDATE"

  val MESSAGE_REGISTER_SUBSCRIBER_GAME_STREAM = "plug_me_in"

  // Constants - Message turn indicator types
  val MESSAGE_TURN_INDICATOR_YOUR_TURN = "YOUR_TURN"
  val MESSAGE_TURN_INDICATOR_WAITING = "WAITING"

  // Constants - Game over message types
  val MESSAGE_GAME_OVER_YOU_WIN = "YOU_WIN"
  val MESSAGE_GAME_OVER_TIED = "TIED"

  val GAME_START_RESPONSE_TURN = "TURN"
}