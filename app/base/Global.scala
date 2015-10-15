import com.typesafe.conductr.bundlelib.play.StatusService
import play.api.{ Application, GlobalSettings }
import com.typesafe.conductr.bundlelib.play.ConnectionContext.Implicits.defaultContext

object Global extends GlobalSettings {
  override def onStart(app: Application): Unit = {
      StatusService.signalStartedOrExit()
  }
}
