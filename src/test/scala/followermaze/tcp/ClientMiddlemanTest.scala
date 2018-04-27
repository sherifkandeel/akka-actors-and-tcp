package followermaze.tcp

import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.io.Tcp._
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import followermaze.handlers._
import followermaze._

import scala.concurrent.duration.FiniteDuration

class ClientMiddlemanTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  "A streamHandler" must {
    "create User by Id and register to connection" in {
      val clientActor = TestProbe()
      val config = Config("test", "akka://MySpec/system" , clientActor.ref.path.name)

      val actorRef = TestActorRef(new ClientMiddleman(config))

      val encodedMessage = ByteString("12")
      actorRef ! encodedMessage

      expectMsg(_:Register)

      actorRef ! PoisonPill
    }

    "forward close message and close itself when sent a close signal" in {
      val middlemanWatcher = TestProbe()
      val userWatcher = TestProbe()
      val config = Config("test", "akka://MySpec/user" , "")

      val actorRef = TestActorRef(new ClientMiddleman(config))
      val userClient = TestActorRef(new UserHandler(actorRef), "default")
      middlemanWatcher watch actorRef
      userWatcher watch userClient

      val closeMessage = Closed
      actorRef ! closeMessage

      expectMsg(_:ConnectionClosed)

      middlemanWatcher.expectTerminated(actorRef)
      userWatcher.expectTerminated(userClient)

      actorRef ! PoisonPill
    }
  }
}

