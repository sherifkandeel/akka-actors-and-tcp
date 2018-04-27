package followermaze.tcp

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp.{ConnectionClosed, Received, Register}
import followermaze.Config
import followermaze.handlers._

class ClientMiddleman(config: Config) extends Actor with ActorLogging {
  var clientId: String = "default"

  override def receive: Receive = {
    case Received(data) =>
      clientId = data.utf8String.trim
      val handler = context.system.actorOf(Props(classOf[UserHandler], sender()), clientId)
      sender() ! Register(handler, keepOpenOnPeerClosed = true)

    case closeMessage: ConnectionClosed =>
      log.debug(s"Middleman $clientId Connection has been closed")
      context.system.actorSelection(s"${config.actor_system_base}/$clientId") ! closeMessage
      context stop self
  }
}
