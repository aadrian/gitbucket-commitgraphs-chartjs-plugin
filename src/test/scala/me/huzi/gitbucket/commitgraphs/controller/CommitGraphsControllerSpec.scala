package me.huzi.gitbucket.commitgraphs.controller

import org.scalatest.funsuite.AnyFunSuite

class CommitGraphsControllerSpec extends AnyFunSuite {

  test("routes register against this GitBucket version") {
    // The routes are registered in the constructor, so authentication helpers missing in core
    // (like adminOnly with a form, renamed in 4.48.0) fail here with NoSuchMethodError
    new CommitGraphsController
  }

  test("sumNumstat sums additions and deletions") {
    val lines = Seq(
      "\"0123456789abcdef0123456789abcdef01234567\"",
      "",
      "10\t2\tREADME.md",
      "3\t0\tsrc/main/scala/Plugin.scala"
    )
    assert(CommitGraphsController.sumNumstat(lines) == ((13, 2)))
  }

  test("sumNumstat skips binary files and other lines") {
    val lines = Seq("-\t-\timage.png", "1\t1\ta.txt", "not\ta numstat line")
    assert(CommitGraphsController.sumNumstat(lines) == ((1, 1)))
  }

  test("sumNumstat of no lines is zero") {
    assert(CommitGraphsController.sumNumstat(Nil) == ((0, 0)))
  }
}
