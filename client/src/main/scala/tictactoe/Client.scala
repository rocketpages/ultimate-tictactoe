package tictactoe

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExport}

@JSExportAll
object Client extends js.JSApp {
  def main(): Unit = {
    dom.console.log("i am alive!")
  }

  def decorate(): Unit = {
    dom.console.log("woo!")
  }
}