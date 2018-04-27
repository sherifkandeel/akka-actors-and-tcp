package followermaze.handlers

import akka.actor.{Actor, ActorLogging, ActorRef}
import followermaze._

class EventBatchHandler(config: Config) extends Actor with ActorLogging {
  private var actors: Array[ActorRef] = Array.empty[ActorRef]

  override def receive: Receive = {
    case batch: String =>
      val messages = batch.split("\n").toList
      processEvents(parseEvents(messages))
  }

  def parseEvents(rawEventList: List[String]) = {
    val currentBatchEvents: List[Event] = rawEventList.map { s =>
      val message = s.trim.split('|')
      message(1) match {
        case "F" => FollowEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "U" => UnFollowEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "B" => BroadCastEvent(Sequence(message(0).toInt))
        case "P" => PrivateMessageEvent(Sequence(message(0).toInt), UserId(message(2).toInt), UserId(message(3).toInt))
        case "S" => StatusUpdateEvent(Sequence(message(0).toInt), UserId(message(2).toInt))
      }
    }
    val sorted = currentBatchEvents.sortBy(_.seq.value)
    sorted
  }

  def processEvents(events: List[Event]) = {
    events foreach {
      case e: FollowEvent         => follow(e)
      case e: UnFollowEvent       => unFollow(e)
      case e: BroadCastEvent      => broadCast(e)
      case e: PrivateMessageEvent => privateMessage(e)
      case e: StatusUpdateEvent   => statusUpdate(e)
      case _ => ()
    }
  }

  def follow(followEvent: FollowEvent) = {
    context.system.actorSelection(constructUserPath(Some(followEvent.from))) ! followEvent
    context.system.actorSelection(constructUserPath(Some(followEvent.to))) ! followEvent
  }

  def unFollow(unFollowEvent: UnFollowEvent) = {
    context.system.actorSelection(constructUserPath(Some(unFollowEvent.from))) ! unFollowEvent
    context.system.actorSelection(constructUserPath(Some(unFollowEvent.to))) ! unFollowEvent
  }

  def
  broadCast(broadCastEvent: BroadCastEvent) = {
    context.system.actorSelection(constructUserPath(None)) ! broadCastEvent
  }

  def privateMessage(privateMessageEvent: PrivateMessageEvent) = {
    context.system.actorSelection(constructUserPath(Some(privateMessageEvent.to))) ! privateMessageEvent
  }

  def statusUpdate(statusUpdateEvent: StatusUpdateEvent) = {
    context.system.actorSelection(constructUserPath(None)) ! statusUpdateEvent
  }

  def constructUserPath(userId: Option[UserId]) = {
    val path = s"${config.actor_system_base}/${userId.map(_.value.toString).getOrElse("*")}"
    path
  }

}
