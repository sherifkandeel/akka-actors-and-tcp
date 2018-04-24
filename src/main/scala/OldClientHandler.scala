//import java.net.InetSocketAddress
//
//import akka.actor.{Actor, ActorRef}
//import akka.io.Tcp
//import akka.io.Tcp._
//import akka.util.ByteString
//
//class OldClientHandler(connection: ActorRef, remote: InetSocketAddress) extends Actor {
//  def receive = {
//    case Received(data) => process(data)
//    case PeerClosed => context stop self
//  }
//
//  def process(data: ByteString): Unit = {
//    println(s"Connected to: ${data.utf8String}")
//    sender() ! Write(ByteString("1|F|14|14"))
////    TCPServer.handlers = TCPServer.handlers :+ ConnectedClient(data.utf8String.trim.toInt, sender())
//  }
//}
