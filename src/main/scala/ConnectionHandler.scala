import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import java.net.InetSocketAddress

import ConnectionHandler.Ack
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.ByteString

object ConnectionHandler {
  final case class Ack(offset: Int) extends Tcp.Event

  def props(connection: ActorRef, remote: InetSocketAddress): Props =
    Props(classOf[ConnectionHandler], connection, remote)
}

class ConnectionHandler(connection: ActorRef, remote: InetSocketAddress)
  extends Actor with ActorLogging {

  import Tcp._

  // sign death pact: this actor terminates when connection breaks
  context watch connection

  def writeWithAck(data: ByteString) = {
//    buffer(data)
//    connection ! Write(byteString, Ack(currentOffset))
    connection ! Write(data)
  }
  // start out in optimistic write-through mode
  override def receive = writingWithEffects(_ => ())
  def writingWithEffects(implicit f: ByteString => Unit): Receive = {
    case Received(data) =>
      f(data)
//      connection ! Write(ByteString.empty, Ack(currentOffset))
      buffer(data)

    case Ack(ack) => acknowledge(ack)

    case CommandFailed(Write(_, Ack(ack))) =>
      connection ! ResumeWriting
      context become buffering(ack)

    case PeerClosed =>
      if (storage.isEmpty) context stop self
      else context become closing
  }

//  def writing: Receive = {
//    case Received(data) =>
//      connection ! Write(data, Ack(currentOffset))
//      buffer(data)
//
//    case Ack(ack) => println("received Ack"); acknowledge(ack)
//
//    case CommandFailed(Write(_, Ack(ack))) =>
//      connection ! ResumeWriting
//      context become buffering(ack)
//
//    case PeerClosed =>
//      if (storage.isEmpty) context stop self
//      else context become closing
//  }

  def buffering(nack: Int)(implicit f: ByteString => Unit): Receive = {
    var toAck = 10
    var peerClosed = false

    {
      case Received(data) => buffer(data)
      case WritingResumed => writeFirst()
      case PeerClosed => peerClosed = true
      case Ack(ack) if ack < nack => acknowledge(ack)
      case Ack(ack) =>
        acknowledge(ack)
        if (storage.nonEmpty) {
          if (toAck > 0) {
            // stay in ACK-based mode for a while
            writeFirst()
            toAck -= 1
          } else {
            // then return to NACK-based again
            writeAll()
            context become (if (peerClosed) closing else writingWithEffects)
          }
        } else if (peerClosed) context stop self
        else context become writingWithEffects
    }
  }

  def closing: Receive = {
    case CommandFailed(_: Write) =>
      connection ! ResumeWriting
      context.become({

        case WritingResumed =>
          writeAll()
          context.unbecome()

        case ack: Int => acknowledge(ack)

      }, discardOld = false)

    case Ack(ack) =>
      acknowledge(ack)
      if (storage.isEmpty) context stop self
  }

  override def postStop(): Unit = {
    log.info(s"transferred $transferred bytes from/to [$remote]")
  }

  private var storageOffset = 0
  private var storage = Vector.empty[ByteString]
  private var stored = 0L
  private var transferred = 0L

  val maxStored = 100000000L
  val highWatermark = maxStored * 5 / 10
  val lowWatermark = maxStored * 3 / 10
  private var suspended = false

  private def currentOffset = storageOffset + storage.size

  private def buffer(data: ByteString): Unit = {
    storage :+= data
    stored += data.size

    if (stored > maxStored) {
      log.warning(s"drop connection to [$remote] (buffer overrun)")
      context stop self

    } else if (stored > highWatermark) {
      log.debug(s"suspending reading at $currentOffset")
      connection ! SuspendReading
      suspended = true
    }
  }

  private def acknowledge(ack: Int): Unit = {
    require(ack == storageOffset, s"received ack $ack at $storageOffset")
    require(storage.nonEmpty, s"storage was empty at ack $ack")

    val size = storage(0).size
    stored -= size
    transferred += size

    storageOffset += 1
    storage = storage drop 1

    if (suspended && stored < lowWatermark) {
      log.debug("resuming reading")
      connection ! ResumeReading
      suspended = false
    }
  }

  private def writeFirst(): Unit = {
    connection ! Write(storage(0), Ack(storageOffset))
  }

  private def writeAll(): Unit = {
    for ((data, i) <- storage.zipWithIndex) {
      connection ! Write(data, Ack(storageOffset + i))
    }
  }
}
