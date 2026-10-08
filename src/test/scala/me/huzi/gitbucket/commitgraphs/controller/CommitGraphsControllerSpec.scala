package me.huzi.gitbucket.commitgraphs.controller

import org.scalatest.funsuite.AnyFunSuite

class CommitGraphsControllerSpec extends AnyFunSuite {

  test("routes register against this GitBucket version") {
    // The routes are registered in the constructor, so authentication helpers missing in core
    // (like adminOnly with a form, renamed in 4.48.0) fail here with NoSuchMethodError
    new CommitGraphsController
  }
}
