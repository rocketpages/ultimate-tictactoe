package actors.messages.akka

import actors.PlayerLetter.PlayerLetter

case class TurnRequest(playerLetter: PlayerLetter, gridNum: String)
