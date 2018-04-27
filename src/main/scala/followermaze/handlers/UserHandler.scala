package followermaze.handlers

import followermaze.tcp._
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString
import followermaze._

class UserHandler(clientConnection: ActorRef) extends Actor with ActorLogging {
  private var followers: List[UserId] = List.empty
  private var followings: List[UserId] = List.empty
  private val id = self.path.name

  override def receive: Receive = {
    case e: FollowEvent => {
      e.to.value.toString match {
        case `id` =>
          followers = followers :+ e.from
          writeToClient(e.toString)
        case _ =>
          followings = followings :+ e.to
      }
    }

    case e: BroadCastEvent => writeToClient(e.toString)

    case e: StatusUpdateEvent => {
      if (followings.contains(e.from)) {
        writeToClient(e.toString)
      }
    }

    case e: UnFollowEvent => {
      e.to.value.toString match {
        case `id` => followers = followers.filterNot(_ == e.from)
        case _    => followings = followings.filterNot(_ == e.to)
      }
    }

    case e: PrivateMessageEvent => {
      writeToClient(e.toString)
    }

    case message: ConnectionClosed =>
      log.debug(s"User ${id} Connection has been closed")
      context stop self
  }

  def writeToClient(data: String) = {
    val encoded = ByteString(data)
    clientConnection ! Write(encoded)
  }

  def getFollowers() = followers
  def getFollowings() = followings

}
