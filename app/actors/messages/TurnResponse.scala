package actors.messages

import actors.GameStatus.GameStatus
import actors.PlayerLetter.PlayerLetter

case class TurnResponse(playerLetter: PlayerLetter, gameStatus: GameStatus)
