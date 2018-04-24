import akka.actor.{ActorLogging, ActorRef}

import scala.collection.immutable.Nil

class EventBatchHandler(rawEventList: List[String]) {
  private var events: List[Event] = List.empty
  def parseEvents () = {
    val currentBatchEvents: List[Event] = rawEventList.map{s =>
      val message = s.split('|')
      message(1) match {
        case "F" => FollowEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "U" => UnFollowEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "B" => BroadCastEvent(Sequence(message(0).toInt))
        case "P" => PrivateMessageEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "S" => StatusUpdateEvent(Sequence(message(0).toInt), UserId(message(2).toInt))
      }
    }
    events = currentBatchEvents.sortBy(_.seq.value)
  }
  def processEvents() = {
    events foreach {
      case e:FollowEvent => follow(e)
      case e:UnFollowEvent => unFollow(e)
      case e:BroadCastEvent => broadCast(e)
      case e:PrivateMessageEvent => privateMessage(e)
      case e:StatusUpdateEvent => statusUpdate(e)
      case _ => ()
    }
  }

  def follow(followEvent: FollowEvent) = {
    UserBase.getUser(followEvent.from) match {
      case Some(u) => u.addFollowing(followEvent.to)
      case None => ()
    }
    notify(followEvent.to, followEvent)
  }

  def unFollow(unFollowEvent: UnFollowEvent) = {
    UserBase.getUser(unFollowEvent.from) match {
      case Some(u) => u.removeFollwing(unFollowEvent.to)
      case None => ()
    }

  }

  def broadCast(broadCastEvent: BroadCastEvent) =  notifyAll(UserBase.getUsers.map(_.id), broadCastEvent)

  def privateMessage(privateMessageEvent: PrivateMessageEvent) = {
    notify(privateMessageEvent.to, privateMessageEvent)
//    UserBase.getUser(privateMessageEvent.to) match {
//      case Some(u) => notify(u.id, privateMessageEvent)
//      case None => ()
//    }
  }

  def statusUpdate(statusUpdateEvent: StatusUpdateEvent) = {
    val usersToNotify = UserBase.getUsers.filter(_.getFollowings.contains(statusUpdateEvent.from)).map(_.id)
    usersToNotify match {
      case Nil => ()
      case usersToNotify =>
        notifyAll(usersToNotify, statusUpdateEvent)
    }
  }

  def notify(userId: UserId, event: Event) = {
    ConnectedClients.send(userId, event.toString)
  }

  def notifyAll(users: List[UserId], event: Event) = {
    users match {
      case Nil => ()
      case users => users foreach(u => notify(u, event))
    }
  }
}
