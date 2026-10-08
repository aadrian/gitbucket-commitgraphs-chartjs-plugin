# GitBucket Commit Graphs Chart.js Plugin

This plugin is a forked version of [gitbucket-commitgraphs-plugin](https://github.com/yoshiyoshifujii/gitbucket-commitgraphs-plugin) that can be used in a closed environment.

In the original version, graphs were created using Google Charts on the Internet.

GitBucket Commit Graphs Chart.js Plugin includes chart.js and can be used in closed environments without internet access.

## Install

- Download  gitbucket-commitgraphs-chartjs-plugin-X.X.X.jar from [Releases](https://github.com/yasumichi/gitbucket-commitgraphs-chartjs-plugin/releases)
- Place the downloaded file in ~/.gitbucket/plugins.

The git command is required.

## Compatibility

Plugin version | GitBucket version
:--------------|:-----------------
0.2.x          | 4.48.x -
0.1.x          | 4.36.x - 4.47.x

GitBucket 4.48.0 changed the authentication helpers used by the settings page, so 0.1.x fails to start there.

## Build

- `sbt assembly`
- Copy `target/scala-2.13/gitbucket-commitgraphs-chartjs-plugin-x.x.x.jar` into `~/.gitbucket/plugins` and restart GitBucket.

## Usage

Click "Commit Graphs(chart.js)" in the repository menu. Then, you will see the graph below.

<img width="830" height="479" alt="commit graph" src="https://github.com/user-attachments/assets/7818164b-f75f-4462-8987-38a7c0f617cb" />

