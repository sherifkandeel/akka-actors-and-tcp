package followermaze.handlers

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import followermaze.handlers._
import followermaze._

class EventBatchHandlerTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  override  def beforeAll: Unit = {

  }

  "BroadCast" must {
    "broadcats to everyone" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val userActor_1 = TestProbe()
      val userActor_2 = TestProbe()
      val userActor_3 = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config))
      val event = BroadCastEvent(Sequence(1))

      val ebh = actorRef.underlyingActor
      ebh.broadCast(event)

      userActor_1 expectMsg (event)
      userActor_2 expectMsg (event)
      userActor_3 expectMsg (event)
    }
  }

  "ParseEvents" must {
    "parse events" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val actorRef = TestActorRef(new EventBatchHandler(config))
      val ebh = actorRef.underlyingActor
      val eventList = List("5|S|2","10|F|1|5", "15|P|1|5")
      val parsed = ebh.parseEvents(eventList)

      parsed.length shouldBe 3
      parsed.head.isInstanceOf[StatusUpdateEvent] shouldBe true
    }

    "sort events" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val actorRef = TestActorRef(new EventBatchHandler(config))
      val ebh = actorRef.underlyingActor
      val eventList = List("5|S|2","10|F|1|5", "1|P|1|5")
      val parsed = ebh.parseEvents(eventList)

      parsed.exists(_.isInstanceOf[StatusUpdateEvent]) shouldBe true
      parsed.exists(_.isInstanceOf[FollowEvent]) shouldBe true
      parsed.exists(_.isInstanceOf[PrivateMessageEvent]) shouldBe true
    }
  }

  "StatusUpdate" must {
    "send status update to everyone" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val userActor = TestProbe()


      val actorRef = TestActorRef(new EventBatchHandler(config))
      val event = StatusUpdateEvent(Sequence(1), UserId(15))

      val ebh = actorRef.underlyingActor
      ebh.statusUpdate(event)

      userActor expectMsg(event)
    }
  }

  "ProcessEvents" must {
    val config = Config("test", "akka://MySpec/system" , "")

    "call follow in case of follow" in {
      val followTestActor = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def follow(e: FollowEvent) = {
          followTestActor.ref ! e
        }})

      val followEvent = FollowEvent(Sequence(1), UserId(1), UserId(2))
      val events = List(followEvent)

      val ebh = actorRef.underlyingActor
      ebh.processEvents(events)

      followTestActor expectMsg(followEvent)
    }

    "call unfollow in case of unfollow" in {
      val unFollowTestActor = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def unFollow(e: UnFollowEvent) = {
          unFollowTestActor.ref ! e
        }})

      val unFollowEvent = UnFollowEvent(Sequence(1), UserId(1), UserId(2))
      val events = List(unFollowEvent)

      val ebh = actorRef.underlyingActor
      ebh.processEvents(events)

      unFollowTestActor expectMsg(unFollowEvent)
    }

    "call privateMesage in case of privateMessage" in {
      val privateMessageActor = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def privateMessage(e: PrivateMessageEvent) = {
          privateMessageActor.ref ! e
        }})

      val privateMessageEvent = PrivateMessageEvent(Sequence(1), UserId(1), UserId(2))
      val events = List(privateMessageEvent)

      val ebh = actorRef.underlyingActor
      ebh.processEvents(events)

      privateMessageActor expectMsg(privateMessageEvent)
    }

    "call broadcast in case of broadcast" in {
      val broadcastActor = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def broadCast(e: BroadCastEvent) = {
          broadcastActor.ref ! e
        }})

      val broadCastEvent = BroadCastEvent(Sequence(1))
      val events = List(broadCastEvent)

      val ebh = actorRef.underlyingActor
      ebh.processEvents(events)

      broadcastActor expectMsg(broadCastEvent)
    }

    "call statusUpdate in case of statusUpdate" in {
      val statusUpdateActor = TestProbe()

      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def statusUpdate(e: StatusUpdateEvent) = {
          statusUpdateActor.ref ! e
        }})

      val statusUpdateEvent = BroadCastEvent(Sequence(1))
      val events = List(statusUpdateEvent)

      val ebh = actorRef.underlyingActor
      ebh.processEvents(events)

      statusUpdateActor expectMsg(statusUpdateEvent)
    }

  }

  "Follow" must {
    "send follow event only to the to and the from" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val userActor_1 = TestProbe()
      val userActor_2 = TestProbe()
      val userActor_3 = TestProbe()


      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def constructUserPath(userId: Option[UserId]) = {
        val path = s"${config.actor_system_base}/testProbe-${userId.map(_.value.toString).getOrElse("*")}"
        path
      }})
      val event = FollowEvent(Sequence(1), UserId(userActor_1.testActor.path.name.split('-')(1).toInt), UserId(userActor_2.testActor.path.name.split('-')(1).toInt))

      val ebh = actorRef.underlyingActor
      ebh.follow(event)

      userActor_1 expectMsg(event)
      userActor_2 expectMsg(event)
      userActor_3 expectNoMessage()
    }
  }

  "UnFollow" must {
    "send unfollow event only to the to and the from" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val userActor_1 = TestProbe()
      val userActor_2 = TestProbe()
      val userActor_3 = TestProbe()


      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def constructUserPath(userId: Option[UserId]) = {
          val path = s"${config.actor_system_base}/testProbe-${userId.map(_.value.toString).getOrElse("*")}"
          path
        }})
      val event = UnFollowEvent(Sequence(1), UserId(userActor_1.testActor.path.name.split('-')(1).toInt), UserId(userActor_2.testActor.path.name.split('-')(1).toInt))

      val ebh = actorRef.underlyingActor
      ebh.unFollow(event)

      userActor_1 expectMsg(event)
      userActor_2 expectMsg(event)
      userActor_3 expectNoMessage()
    }
  }

  "PrivateMessage" must {
    "send private message event only to the to" in {
      val config = Config("test", "akka://MySpec/system" , "")
      val userActor_1 = TestProbe()
      val userActor_2 = TestProbe()
      val userActor_3 = TestProbe()


      val actorRef = TestActorRef(new EventBatchHandler(config) {
        override def constructUserPath(userId: Option[UserId]) = {
          val path = s"${config.actor_system_base}/testProbe-${userId.map(_.value.toString).getOrElse("*")}"
          path
        }})
      val event = PrivateMessageEvent(Sequence(1), UserId(userActor_1.testActor.path.name.split('-')(1).toInt), UserId(userActor_2.testActor.path.name.split('-')(1).toInt))

      val ebh = actorRef.underlyingActor
      ebh.privateMessage(event)

      userActor_1 expectNoMessage()
      userActor_2 expectMsg(event)
      userActor_3 expectNoMessage()
    }
  }

}
