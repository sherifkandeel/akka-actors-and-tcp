import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

import akka.io.Tcp._

class Server(address: String, port: Int, t:Class[_]) extends Actor{
  import context.system
  val manager = IO(Tcp)
  manager ! Bind(self, new InetSocketAddress(address, port))
  override def receive = {
    case b @ Bound(localAddress) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context stop self

    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props(t, sender(), remote))
      val connection = sender()
      connection ! Register(handler, keepOpenOnPeerClosed = true)
//      TCPServer.handlers = TCPServer.handlers :+ sender()

//    case message: ConnectionClosed =>
//      println("Connection has been closed")
//      context stop self
  }
}
object TCPServer extends App {
  val system = ActorSystem()
  val tcpServer1 = system.actorOf(Props(classOf[Server], "localhost", 9090, classOf[EventStreamHandler]))
  val tcpServer2 = system.actorOf(Props(classOf[Server], "localhost", 9099, classOf[ClientHandler]))
//  val tcpServer2 = system.actorOf(Props(classOf[Server], "localhost", 9099, classOf[OldClientHandler]))


//  var handlers: List[ConnectedClient] = List.empty

}

