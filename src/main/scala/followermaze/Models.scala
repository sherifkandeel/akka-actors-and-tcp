package followermaze


case class UserId(value: Int) extends AnyVal

case class Sequence(value: Int) extends AnyVal

trait Event {
  def seq: Sequence
}

case class FollowEvent(seq: Sequence, from: UserId, to: UserId) extends Event {
  override def toString: String = s"${seq.value}|F|${from.value}|${to.value}\n"
}

case class UnFollowEvent(seq: Sequence, from: UserId, to: UserId) extends Event {
  override def toString: String = s"${seq.value}|U|${from.value}|${to.value}\n"
}

case class BroadCastEvent(seq: Sequence) extends Event {
  override def toString: String = s"${seq.value}|B\n"
}

case class PrivateMessageEvent(seq: Sequence, from: UserId, to: UserId) extends Event {
  override def toString: String = s"${seq.value}|P|${from.value}|${to.value}\n"
}

case class StatusUpdateEvent(seq: Sequence, from: UserId) extends Event {
  override def toString: String = s"${seq.value}|S|${from.value}\n"
}

case class StatusUpdateMessage(seq: Sequence, from: UserId) extends Event {
  override def toString: String = s"${seq.value}|S|${from.value}\n"
}

case class Config(
  actor_system_name: String,
  actor_system_base: String,
  event_handler_name: String
)
