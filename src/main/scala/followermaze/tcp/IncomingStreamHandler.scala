package followermaze.tcp

import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp.{ConnectionClosed, Received}
import followermaze.Config
import followermaze.handlers._

class IncomingStreamHandler(config: Config) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Received(data) =>
      val messages = data.utf8String
      context.actorSelection(s"${config.actor_system_base}/${config.event_handler_name}") ! messages

    case message: ConnectionClosed =>
      log.debug("Event stream connection has been closed")
      context stop self
  }
}
