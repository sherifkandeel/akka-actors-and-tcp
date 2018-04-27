package followermaze.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import followermaze.Config
import followermaze.handlers._

class Server(address: String, port: Int, config: Config, t: Class[_]) extends Actor {

  import context.system

  val manager = IO(Tcp)
  manager ! Bind(self, new InetSocketAddress(address, port))

  override def receive = {
    case b@Bound(localAddress) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      val handler = context.actorOf(Props(t, config))
      val connection = sender()
      connection ! Register(handler, keepOpenOnPeerClosed = true)

    case message: ConnectionClosed =>
      context stop self
  }
}

object TCPServer extends App {
  val config = Config("follower-maze", "akka://follower-maze/user","eventBatchHandler")

  val system = ActorSystem(config.actor_system_name)
  val tcpServer1 = system.actorOf(Props(classOf[Server], "localhost", 9090, config, classOf[IncomingStreamHandler]))
  val tcpServer2 = system.actorOf(Props(classOf[Server], "localhost", 9099, config, classOf[ClientMiddleman]))
  val eventBatchHandler = system.actorOf(Props(classOf[EventBatchHandler], config), config.event_handler_name)
}

