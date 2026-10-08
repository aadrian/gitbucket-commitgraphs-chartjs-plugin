import gitbucket.core.plugin.Link
import io.github.gitbucket.solidbase.model.Version
import gitbucket.core.controller.Context
import gitbucket.core.service.RepositoryService.RepositoryInfo
import me.huzi.gitbucket.commitgraphs.controller.CommitGraphsController

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "commitgraphs-chartjs"

  override val pluginName: String = "Commit Graphs(chart.js)"

  override val description: String = "Viewing the commit count in the chart.js graph."

  override val versions: Seq[Version] = Seq(
    new Version("0.1.0"),
    new Version("0.1.1"),
    new Version("0.2.0")
  )

  override val controllers = Seq(
    "/*" -> new CommitGraphsController
  )

  override val assetsMappings = Seq("/commitgraphs-chartjs" -> "/commitgraphs-chartjs/assets")

  override val repositoryMenus = Seq((repository: RepositoryInfo, context: Context) =>
    Some(
      Link(
        id = pluginId,
        label = pluginName,
        path = s"/commitgraphs-chartjs",
        icon = Some("menu-icon octicon octicon-graph")
      )
    )
  )

  override val systemSettingMenus: Seq[(Context) => Option[Link]] =
    Seq((ctx: Context) => Some(Link(pluginId, pluginName, "admin/commitgraphs-chartjs", Some("octicon octicon-graph"))))

}
