package actors.messages

import actors.PlayerLetter.PlayerLetter

case class TurnRequest(playerLetter: PlayerLetter, gridNum: String)
