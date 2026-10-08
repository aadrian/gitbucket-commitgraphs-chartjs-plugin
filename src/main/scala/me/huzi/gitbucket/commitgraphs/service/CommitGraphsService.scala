package me.huzi.gitbucket.commitgraphs.service

import java.io.File
import scala.jdk.CollectionConverters._
import scala.sys.process._
import org.eclipse.jgit.api.Git
import gitbucket.core.util.JGitUtil.CommitInfo
import me.huzi.gitbucket.commitgraphs.service.CommitGraphsService._

trait CommitGraphsService {

  /** Commit statistics per contributor over all refs; runs `gitCommand` once per commit for the line counts. */
  def getCommitGraphs(git: Git, gitCommand: String): Seq[CommitGraph] = {
    val dir = git.getRepository.getDirectory
    val counts = git.log.all.call.iterator.asScala.map { rev =>
      val (additions, deletions) = numstat(gitCommand, dir, rev.getId.name)
      CommitCount(new CommitInfo(rev), additions, deletions)
    }.toSeq
    aggregate(counts)
  }
}

object CommitGraphsService {

  /** Added and deleted lines of one commit, from `git log --numstat`. */
  def numstat(gitCommand: String, gitDir: File, commitId: String): (Int, Int) = {
    val p = Process(
      Seq(gitCommand, "log", "-n", "1", "--numstat", """--pretty="%H"""", "--source", commitId),
      gitDir
    )
    sumNumstat(p.lazyLines_!)
  }

  /** Sums the added and deleted lines of `git log --numstat` output; binary files ("-") are skipped. */
  def sumNumstat(lines: Iterable[String]): (Int, Int) =
    lines.filter(_.split("\t").length == 3).foldLeft((0, 0)) { (i, l) =>
      val Array(a, d, filename) = l.split("\t")
      if (a.forall(_.isDigit) && d.forall(_.isDigit)) {
        (i._1 + a.toInt, i._2 + d.toInt)
      } else {
        i
      }
    }

  /** Whether `command` runs; git without arguments prints its usage and exits with 1. */
  def isGitAvailable(command: String): Boolean = {
    try {
      Process(command).!(ProcessLogger(line => ())) == 1
    } catch {
      case _: Throwable => false
    }
  }

  /** Groups the commits per contributor (most commits first), with daily counts in date order. */
  def aggregate(counts: Seq[CommitCount]): Seq[CommitGraph] =
    counts
      .groupBy(_.groupKey)
      .toSeq
      .sortWith((lt1, lt2) => lt1._2.lengthCompare(lt2._2.length) > 0)
      .map { case (mailAddress, ds) =>
        val dh = ds.head
        val dailys = ds
          .groupBy(x => date2DateStr(x.authorTime))
          .map { case (date, ds) =>
            val (additions, deletions) = ds.foldLeft((0L, 0L)) { (i, d) =>
              (i._1 + d.additions, i._2 + d.deletions)
            }
            DailyCount(
              userName = dh.authorName,
              mailAddress = dh.authorEmailAddress,
              date = dateStr2Date(date),
              commits = ds.length,
              additions = additions,
              deletions = deletions
            )
          }
          .toSeq
          .sortWith((lt1, lt2) => lt1.date.compareTo(lt2.date) < 0)

        val (additions, deletions) = dailys.foldLeft((0L, 0L)) { (i, d) =>
          (i._1 + d.additions, i._2 + d.deletions)
        }
        CommitGraph(
          userName = dh.authorName,
          mailAddress = mailAddress,
          commits = ds.length,
          additions = additions,
          deletions = deletions,
          dailys = dailys
        )
      }

  private def date2DateStr(date: java.util.Date): String = new java.text.SimpleDateFormat("yyyy-MM-dd").format(date)
  private def dateStr2Date(date: String): java.util.Date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(date)
}

/** One commit: who it counts for, and its line counts. */
case class CommitCount(
    groupKey: String,
    authorName: String,
    authorEmailAddress: String,
    authorTime: java.util.Date,
    additions: Long,
    deletions: Long
)

object CommitCount {
  def apply(commit: CommitInfo, additions: Long, deletions: Long): CommitCount =
    CommitCount(
      groupKey = commit.authorEmailAddress,
      authorName = commit.authorName,
      authorEmailAddress = commit.authorEmailAddress,
      authorTime = commit.authorTime,
      additions = additions,
      deletions = deletions
    )
}

case class DailyCount(
    userName: String,
    mailAddress: String,
    date: java.util.Date,
    commits: Long,
    additions: Long,
    deletions: Long
)

case class CommitGraph(
    userName: String,
    mailAddress: String,
    commits: Long,
    additions: Long,
    deletions: Long,
    dailys: Seq[DailyCount]
)
