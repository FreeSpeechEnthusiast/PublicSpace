package com.twitter.auth.policy

import com.twitter.configbus.client.ConfigSnapshot
import com.twitter.configbus.client.ConfigSource
import com.twitter.io.Buf
import com.twitter.util.Activity
import com.twitter.util.Future
import com.twitter.util.Time
import com.twitter.util.Updatable
import com.twitter.util.Var
import scala.collection.mutable

class MockConfigSource extends ConfigSource {
  private val perPathVars =
    mutable.Map[
      String,
      Var[Activity.State[ConfigSnapshot[Buf]]] with Updatable[
        Activity.State[ConfigSnapshot[Buf]]
      ]]()

  def setFileContents(path: String, content: String) = {
    val buf: Buf = Buf.Utf8(content)
    val v = perPathVars.getOrElseUpdate(path, Var(Activity.Pending))
    v.update(Activity.Ok(ConfigSnapshot(None, buf)))
  }

  override def subscribe(path: String): Activity[ConfigSnapshot[Buf]] = synchronized {
    val v = perPathVars.getOrElseUpdate(path, Var(Activity.Pending))
    Activity(v)
  }

  override def subscribeRecursive(
    parent: String,
    glob: Option[String]
  ): Activity[ConfigSnapshot[Map[String, Buf]]] = ???

  override def close(deadline: Time): Future[Unit] = Future.Unit
}
