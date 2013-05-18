package lila.hub

import actorApi.map._

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import makeTimeout.short

final class ActorMap[A <: Actor](mkActor: String ⇒ A) extends Actor {

  def receive = {

    case Get(id) ⇒ sender ! {
      loginfo("get actor " + id)
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor ⇒
          loginfo("create actor " + id)
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Count ⇒ sender ! actors.size

    case Do(id, f) ⇒ self ? Get(id) mapTo manifest[ActorRef] foreach f 

    case Ask(id, msg) ⇒
      self ? Get(id) mapTo manifest[ActorRef] flatMap { _ ? msg } pipeTo sender

    case Stop(id) ⇒ "stop actor " + id describes {
      actors get id foreach context.stop
    }

    case Terminated(actor) ⇒ actors find (_._2 == actor) foreach {
      case (id, _) ⇒ "remove actor " + id describes {
        actors = actors - id
      }
    }
  }

  private var actors = Map[String, ActorRef]()
}
