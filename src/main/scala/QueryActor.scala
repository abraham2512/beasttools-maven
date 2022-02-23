//import HdfsRegistry.SpeakText
//import akka.actor.typed.Behavior
//import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
//import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
//
//object QueryActor {
//  sealed trait QueryCommand
//  val QueryKey: ServiceKey[QueryCommand] = ServiceKey("QUERY_ACTOR")
//
//  final case class SpeakText(message:String) extends QueryCommand
//  final case class RunQuery(file: File,Query:String) extends QueryCommand
//  final case class QueryActionPerformed(message:String) extends QueryCommand
//
//  def apply(): Behavior[QueryCommand] = Behaviors.setup {
//
//    context: ActorContext[QueryCommand] =>
//    context.system.receptionist ! Receptionist.Register(QueryKey,context.self)
//    println("QueryActor: Query Actor Born!")
//
////    Behaviors.receiveMessage {
////      case SpeakText(msg) =>
////        println(s"QueryActor: got a msg: $msg")
////      Behaviors.same
////
////    case RunQuery(file,query) =>
////      println(s"running query: $query")
////      val status = DataFileDAL.get(file.filename).get._4 //Getting status
////
////      println(status)
////
////      Behaviors.same
////
////
////    }
//
//    QueryActionPerformed("Done")
//    Behaviors.same
//  }
//
//
//
//}
