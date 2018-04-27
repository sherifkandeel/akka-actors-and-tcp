package followermaze.tcp


import java.net.InetSocketAddress

import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.io.Tcp._
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import followermaze.handlers._
import followermaze._

class ServerTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  "Server" must {
    "stop server if CommandFailed happened" in {
      val serverWatcher = TestProbe()
      val config = Config("test", "akka://MySpec/user" , "")

      val serverActorRef = TestActorRef(new Server("", 9009, config, classOf[ClientMiddleman]))
      serverWatcher watch serverActorRef

      val closeMessage = CommandFailed
      serverActorRef ! closeMessage

      serverWatcher.expectTerminated(serverActorRef)
    }


    "stop server in case of connectionClosed signal" in {
      val serverWatcher = TestProbe()
      val config = Config("test", "akka://MySpec/user" , "")

      val serverActorRef = TestActorRef(new Server("", 9009, config, classOf[ClientMiddleman]))
      serverWatcher watch serverActorRef

      val closeMessage = Closed
      serverActorRef ! closeMessage

      serverWatcher.expectTerminated(serverActorRef)
    }
  }

}
