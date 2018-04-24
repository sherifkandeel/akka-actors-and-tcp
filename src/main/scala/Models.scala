import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString

object ConnectedClients{
  private var connectedClientsHandlers: List[ConnectedClient] = List.empty
  def add(cc: ConnectedClient): Unit = {
    ConnectedClients.connectedClientsHandlers = ConnectedClients.connectedClientsHandlers :+ cc
    UserBase.addUser(User(cc.id))
//    println(s"Added user: ${cc.id}")
  }
  def getClientHandlers() = connectedClientsHandlers

  def send(userId: UserId, msg: String) = {
    connectedClientsHandlers.find(_.id == userId) match {
//      case Some(c) => c.ref ! Write(ByteString(msg))
      case Some(c) => c.ref.asInstanceOf[ConnectionHandler].writeWithAck(ByteString(msg))
      case None => ()
    }
  }
}

object UserBase{
  private var users: List[User] = List.empty
  def addUser(user: User) = {
    users = users :+ user
  }

  def getUser(userId: UserId) = users.find(_.id == userId)

  def getUsers() = users
}

case class User(id: UserId) {
  private var f: List[UserId] = List.empty
  def addFollowing(fId: UserId): Unit = {
    f = f :+ fId
  }
  def removeFollwing(fId: UserId): Unit = {
    f = f.filterNot(_ == fId)
  }
  def getFollowings() = f
}

case class ConnectedClient(id: UserId, ref: ConnectionHandler)

case class UserId(value: Int) extends AnyVal
case class Sequence(value: Int) extends AnyVal

trait Event {
  def seq: Sequence
}
case class FollowEvent(seq: Sequence, from: UserId, to: UserId) extends Event {
  override def toString: String = s"${seq.value}|F|${from.value}|${to.value}\n"
}
case class UnFollowEvent(seq: Sequence, from: UserId, to: UserId) extends Event{
  override def toString: String = s"${seq.value}|U|${from.value}|${to.value}\n"
}
case class BroadCastEvent(seq: Sequence) extends Event{
  override def toString: String = s"${seq.value}|B\n"
}
case class PrivateMessageEvent(seq: Sequence, from: UserId, to: UserId) extends Event{
  override def toString: String = s"${seq.value}|P|${from.value}|${to.value}\n"
}
case class StatusUpdateEvent(seq: Sequence, from: UserId) extends Event{
  override def toString: String = s"${seq.value}|S|${from.value}\n"
}

