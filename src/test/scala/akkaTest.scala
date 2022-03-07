import actors.{FileRegistry, TileActor}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class akkaTest
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {
  val testKit = ActorTestKit()
  val pinger = testKit.spawn(FileRegistry(),"FileActor")
  val ponger = testKit.spawn(TileActor(),"TileActor")
  val probe = testKit.createTestProbe[Array[Byte]]()
  val dataset = "SafteyDept"

  ponger ! TileActor.GetTile(dataset,("1","2","3"),_)
  probe.expectMessage(Array[Byte])

}