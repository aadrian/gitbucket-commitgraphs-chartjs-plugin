package me.huzi.gitbucket.commitgraphs.service

import org.scalatest.funsuite.AnyFunSuite
import me.huzi.gitbucket.commitgraphs.service.CommitGraphsSettingsService.CommitGraphsSettings

class CommitGraphsSettingsServiceSpec extends AnyFunSuite {

  private val service = new CommitGraphsSettingsService {}

  private def withoutConf(f: => Unit): Unit = {
    service.CommitGraphsConf.getParentFile.mkdirs()
    service.CommitGraphsConf.delete()
    try f
    finally service.CommitGraphsConf.delete()
  }

  test("defaults to git without a settings file") {
    withoutConf {
      assert(service.loadCommitGraphsSettings() == CommitGraphsSettings("git", "check"))
    }
  }

  test("saved git command is loaded again") {
    withoutConf {
      service.saveCommitGraphsSettings(CommitGraphsSettings("/usr/local/bin/git", "apply"))
      assert(service.loadCommitGraphsSettings() == CommitGraphsSettings("/usr/local/bin/git", "check"))
    }
  }
}
