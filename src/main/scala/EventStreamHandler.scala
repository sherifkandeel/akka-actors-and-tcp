import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{Received, Write}
import akka.util.ByteString

class EventStreamHandler(connection: ActorRef, remote: InetSocketAddress)
  extends ConnectionHandler(connection, remote) with Actor with ActorLogging {
  override def receive: Receive = {
    super.writingWithEffects(process)
//    case Received(data) => process(data)
//    super.writing
  }

  def process = { data: ByteString =>
    val message = data.utf8String
//    log.debug(s"message received, batch size: ${message.split("\n").length}")
//    log.debug(s"number of connected clients: ${ConnectedClients.getClientHandlers().length} ")

    val messages = message.split("\n").toList
    //batch here
    val evh = new EventBatchHandler(messages)
    evh.parseEvents()
    evh.processEvents()
    ()
  }
}
