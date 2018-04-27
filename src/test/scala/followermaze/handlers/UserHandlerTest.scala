package followermaze.handlers

import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.io.Tcp.{NoAck, Write}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import followermaze.handlers._
import followermaze._

class UserHandlerTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  "Receive" must {
    "add to followers and write to client in case of new followers" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val clientActor = TestProbe()

      val actorRef = TestActorRef(new UserHandler(clientActor.ref),"2")

      val followEvent = FollowEvent(Sequence(1), UserId(1), UserId(2))
      actorRef ! followEvent

      val userActor = actorRef.underlyingActor
      userActor.getFollowers.length shouldBe 1

      val encoded = Write(ByteString(followEvent.toString),NoAck(null))
      clientActor expectMsg (encoded)

      actorRef ! PoisonPill
    }

    "add to following in case of follow" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val clientActor = TestProbe()

      val actorRef = TestActorRef(new UserHandler(clientActor.ref),"1")

      val followEvent = FollowEvent(Sequence(1), UserId(1), UserId(2))
      actorRef ! followEvent

      val userActor = actorRef.underlyingActor
      userActor.getFollowings.length shouldBe 1

      actorRef ! PoisonPill
    }


    "write message in case of relevant private message" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val clientActor = TestProbe()

      val actorRef = TestActorRef(new UserHandler(clientActor.ref),"2")

      val privateMessageEvent = PrivateMessageEvent(Sequence(1), UserId(1), UserId(2))
      actorRef ! privateMessageEvent

      val encoded = Write(ByteString(privateMessageEvent.toString),NoAck(null))
      clientActor expectMsg (encoded)

      actorRef ! PoisonPill
    }


    "write a broadcast" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val clientActor = TestProbe()

      val actorRef = TestActorRef(new UserHandler(clientActor.ref),"1")

      val broadCastEvent = BroadCastEvent(Sequence(1))
      actorRef ! broadCastEvent

      val encoded = Write(ByteString(broadCastEvent.toString),NoAck(null))
      clientActor expectMsg (encoded)

      actorRef ! PoisonPill
    }

    "[non-unit] get a follower and write to followers your status update" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val clientActor = TestProbe()

      val actorRef = TestActorRef(new UserHandler(clientActor.ref),"1")

      val followEvent = FollowEvent(Sequence(1), UserId(1), UserId(2))
      val statusUpdateEvent = StatusUpdateEvent(Sequence(1), UserId(2))

      actorRef ! followEvent
      actorRef ! statusUpdateEvent

      val encoded = Write(ByteString(statusUpdateEvent.toString),NoAck(null))
      clientActor expectMsg (encoded)

      actorRef ! PoisonPill
    }


  }

}
