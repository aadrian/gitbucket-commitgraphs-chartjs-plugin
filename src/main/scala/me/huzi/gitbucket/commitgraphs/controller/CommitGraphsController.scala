package me.huzi.gitbucket.commitgraphs.controller

import java.io.IOException
import scala.util.Using
import org.eclipse.jgit.api.Git
import gitbucket.core.controller._
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.util.Directory._
import gitbucket.core.util.Implicits._
import org.scalatra.forms._
import org.slf4j.LoggerFactory
import me.huzi.gitbucket.commitgraphs.service.{CommitGraphsService, CommitGraphsSettingsService}
import me.huzi.gitbucket.commitgraphs.service.CommitGraphsSettingsService._
import me.huzi.gitbucket.commitgraphs.html

class CommitGraphsController
    extends CommitGraphsControllerBase
    with RepositoryService
    with AccountService
    with ReferrerAuthenticator
    with AdminAuthenticator
    with CommitGraphsService
    with CommitGraphsSettingsService

trait CommitGraphsControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with ReferrerAuthenticator
    with AdminAuthenticator
    with CommitGraphsService
    with CommitGraphsSettingsService =>

  private val logger = LoggerFactory.getLogger(classOf[CommitGraphsControllerBase])

  val settingsForm: MappingValueType[CommitGraphsSettings] = mapping(
    "commitGraphsGitCommand" -> text(required, maxlength(200)),
    "action" -> text(required, maxlength(10))
  )(CommitGraphsSettings.apply)

  get("/admin/commitgraphs-chartjs")(adminOnly {
    val settings = loadCommitGraphsSettings()
    html.settings(settings.CommitGraphsGitCommand, None)
  })

  post("/admin/commitgraphs-chartjs", settingsForm)(adminOnlyWithForm { form =>
    assert(form.CommitGraphsGitCommand != null)
    assert(!form.CommitGraphsGitCommand.isEmpty)
    form.action match {
      case "apply" =>
        saveCommitGraphsSettings(form)
        html.settings(form.CommitGraphsGitCommand, Some("Settings Saved"))
      case _ =>
        if (CommitGraphsService.isGitAvailable(form.CommitGraphsGitCommand)) {
          html.settings(form.CommitGraphsGitCommand, Some("command check succeded"))
        } else {
          html.settings(form.CommitGraphsGitCommand, Some("no such command in path"))
        }
    }
  })

  get("/:owner/:repository/commitgraphs-chartjs")(referrersOnly { repository =>
    Using.resource(Git.open(getRepositoryDir(repository.owner, repository.name))) { git =>
      if (JGitUtil.isEmpty(git)) {
        html.guide(repository)
      } else {
        val gitCommand = loadCommitGraphsSettings().CommitGraphsGitCommand
        try {
          html.list(repository, getCommitGraphs(git, gitCommand))
        } catch {
          case e: IOException =>
            logger.warn(s"Running '$gitCommand' for ${repository.owner}/${repository.name} failed", e)
            html.error(repository)
        }
      }
    }
  })
}
