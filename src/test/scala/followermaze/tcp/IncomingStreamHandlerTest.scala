package followermaze.tcp


import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.io.Tcp.{NoAck, Received, Write}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import followermaze.handlers._
import followermaze._

class IncomingStreamHandlerTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  "A streamHandler" must {

    "forward message to event handler" in {
      val clientActor = TestProbe()
      val config = Config("test", clientActor.ref.path.parent.toString , clientActor.ref.path.name)
      system.log.debug(clientActor.ref.path.parent.toString)
      system.log.debug(clientActor.ref.path.name)

      val actorRef = TestActorRef(new IncomingStreamHandler(config))

      val message = ByteString("1|F|1|2")
      actorRef ! Received(message)

      clientActor expectMsg(message.utf8String)
      actorRef ! PoisonPill
    }
  }
}
