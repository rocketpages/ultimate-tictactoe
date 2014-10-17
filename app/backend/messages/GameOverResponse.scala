package backend.messages

case class GameOverResponse(tied: Boolean, winningPlayer: Option[String])
