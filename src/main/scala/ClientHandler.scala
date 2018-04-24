import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{Received, Write}
import akka.util.ByteString

class ClientHandler (connection: ActorRef, remote: InetSocketAddress)
  extends ConnectionHandler(connection, remote) with Actor with ActorLogging {
  override def receive: Receive = {
    super.writingWithEffects(process)
//    case Received(data) => process(data)
//    super.writing
  }

  def process = {data: ByteString =>
//    ConnectedClients.add(ConnectedClient(UserId(data.utf8String.trim.toInt), connection))
    ConnectedClients.add(ConnectedClient(UserId(data.utf8String.trim.toInt), this))
  }
}
