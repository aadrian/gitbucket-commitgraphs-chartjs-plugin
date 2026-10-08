package me.huzi.gitbucket.commitgraphs.service

import java.nio.file.{Files, Path}
import java.time.{LocalDate, ZoneId}
import java.util.Date
import scala.util.Using
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.scalatest.funsuite.AnyFunSuite
import gitbucket.core.model.Account
import gitbucket.core.service.AccountService
import gitbucket.core.util.FileUtil
import gitbucket.core.util.JGitUtil.CommitInfo

class CommitGraphsServiceSpec extends AnyFunSuite {

  private def day(d: String, hour: Int = 12): Date =
    Date.from(LocalDate.parse(d).atTime(hour, 0).atZone(ZoneId.systemDefault).toInstant)

  private def count(email: String, date: String, additions: Long, deletions: Long = 0, hour: Int = 12) =
    CommitCount(email.takeWhile(_ != '@'), email, day(date, hour), additions, deletions)

  private def account(userName: String, mailAddress: String) =
    Account(
      userName = userName,
      fullName = userName,
      mailAddress = mailAddress,
      password = "",
      isAdmin = false,
      url = None,
      registeredDate = day("2026-01-01"),
      updatedDate = day("2026-01-01"),
      lastLoginDate = None,
      image = None,
      isGroupAccount = false,
      isRemoved = false,
      description = None
    )

  test("sumNumstat sums additions and deletions") {
    val lines = Seq(
      "\"0123456789abcdef0123456789abcdef01234567\"",
      "",
      "10\t2\tREADME.md",
      "3\t0\tsrc/main/scala/Plugin.scala"
    )
    assert(CommitGraphsService.sumNumstat(lines) == ((13, 2)))
  }

  test("sumNumstat skips binary files and other lines") {
    val lines = Seq("-\t-\timage.png", "1\t1\ta.txt", "not\ta numstat line")
    assert(CommitGraphsService.sumNumstat(lines) == ((1, 1)))
  }

  test("sumNumstat of no lines is zero") {
    assert(CommitGraphsService.sumNumstat(Nil) == ((0, 0)))
  }

  test("aggregate of no commits is empty") {
    assert(CommitGraphsService.aggregate(Nil).isEmpty)
  }

  test("aggregate sums per contributor and per day, most commits first") {
    val graphs = CommitGraphsService.aggregate(
      Seq(
        count("bob@example.com", "2026-09-02", 7),
        count("alice@example.com", "2026-09-02", 2, 1, hour = 9),
        count("alice@example.com", "2026-09-01", 1),
        count("alice@example.com", "2026-09-02", 3, 2, hour = 18)
      )
    )
    assert(
      graphs.map(g => (g.mailAddress, g.commits, g.additions, g.deletions)) == Seq(
        ("alice@example.com", 3, 6, 3),
        ("bob@example.com", 1, 7, 0)
      )
    )
    val alice = graphs.head
    assert(alice.userName == "alice")
    assert(
      alice.dailys.map(d => (d.date, d.commits, d.additions, d.deletions)) == Seq(
        (day("2026-09-01", 0), 1, 1, 0),
        (day("2026-09-02", 0), 2, 5, 3)
      )
    )
  }

  test("aggregate groups all addresses of a GitBucket account, ignoring case") {
    val alice = account("alice", "alice@example.com")
    val accounts = Map("alice@example.com" -> alice, "alice.private@example.org" -> alice)
    val graphs = CommitGraphsService.aggregate(
      Seq(
        count("alice@example.com", "2026-09-01", 1),
        count("Alice@Example.com", "2026-09-02", 2),
        count("alice.private@example.org", "2026-09-03", 3),
        count("bob@example.com", "2026-09-01", 4)
      ),
      mail => accounts.get(mail.toLowerCase)
    )
    assert(
      graphs.map(g => (g.userName, g.mailAddress, g.commits, g.additions)) == Seq(
        ("alice", "alice@example.com", 3, 6),
        ("bob", "bob@example.com", 1, 4)
      )
    )
  }

  test("aggregate groups addresses without an account ignoring case") {
    val graphs = CommitGraphsService.aggregate(
      Seq(count("bob@example.com", "2026-09-01", 1), count("Bob@Example.com", "2026-09-02", 2))
    )
    assert(graphs.map(g => (g.commits, g.additions)) == Seq((2, 3)))
  }

  test("a commit counts for its author") {
    val c = CommitCount(commitInfo("Alice", "alice@example.com", "Alice", "alice@example.com"), 1, 0)
    assert((c.authorName, c.authorEmailAddress) == (("Alice", "alice@example.com")))
  }

  test("a commit committed by someone else (rebase, cherry-pick) counts for its author") {
    val c = CommitCount(commitInfo("Bob", "bob@example.com", "Carol", "carol@example.com"), 1, 0)
    assert((c.authorName, c.authorEmailAddress) == (("Bob", "bob@example.com")))
  }

  test("getCommitGraphs reads the line counts with git") {
    withRepo { (git, dir) =>
      commit(git, dir, "a.txt", "1\n2\n", "Alice", "alice@example.com", "2026-09-01")
      commit(git, dir, "a.txt", "1\n", "Alice", "alice@example.com", "2026-09-02")
      commit(git, dir, "b.txt", "b\n", "Bob", "bob@example.com", "2026-09-02")

      val graphs =
        CommitGraphsService.aggregate(new CommitGraphsService with AccountService {}.getCommitCounts(git, "git"))
      assert(
        graphs.map(g => (g.userName, g.commits, g.additions, g.deletions)) == Seq(
          ("Alice", 2, 2, 1),
          ("Bob", 1, 1, 0)
        )
      )
      assert(graphs.head.dailys.map(_.date) == Seq(day("2026-09-01", 0), day("2026-09-02", 0)))
    }
  }

  test("aggregate orders contributors with the same number of commits by name") {
    val graphs = CommitGraphsService.aggregate(
      Seq("dave", "Carol", "bob", "alice", "Eve").map(n => count(s"$n@example.com", "2026-09-01", 1))
    )
    assert(graphs.map(_.userName) == Seq("alice", "bob", "Carol", "dave", "Eve"))
  }

  test("isGitAvailable") {
    assert(CommitGraphsService.isGitAvailable("git"))
    assert(!CommitGraphsService.isGitAvailable("/nonexistent/git"))
  }

  private def commitInfo(authorName: String, authorEmail: String, committerName: String, committerEmail: String) =
    CommitInfo(
      "0" * 40,
      "message",
      "message",
      Nil,
      day("2026-09-01"),
      authorName,
      authorEmail,
      day("2026-09-01"),
      committerName,
      committerEmail,
      None,
      None
    )

  private def withRepo(f: (Git, Path) => Unit): Unit = {
    val dir = Files.createTempDirectory("commitgraphs")
    try Using.resource(Git.init.setDirectory(dir.toFile).setInitialBranch("main").call())(f(_, dir))
    finally FileUtil.deleteRecursively(dir.toFile)
  }

  private def commit(git: Git, dir: Path, file: String, content: String, name: String, email: String, date: String) = {
    Files.writeString(dir.resolve(file), content)
    git.add.addFilepattern(file).call()
    val ident = new PersonIdent(name, email, day(date).toInstant, ZoneId.systemDefault)
    git.commit.setMessage(s"$name $date").setAuthor(ident).setCommitter(ident).setSign(false).call()
  }
}
