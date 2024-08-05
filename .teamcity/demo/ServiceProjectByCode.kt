package demo

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

class ServiceProjectByCode(
    projectName: String,
    repoUrl: String,
    buildInitializer: (BuildType) -> Unit
) : Project({
    name = projectName

    id(projectName.toId())

    val vcsRoot = GitVcsRoot {
        name = "$projectName repository"

        id("${projectName.toId()}_repo")

        url = repoUrl

        authMethod = customPrivateKey {
            customKeyPath = "TODO"
        }
    }

    vcsRoot(vcsRoot)

    var build: BuildType? = null

    subProject { // this project is accessible to developers
        name = "Development $projectName"

        id("${projectName.toId()}_dev")

        build = buildType {
            name = "Build"

            id("${projectName.toId()}_dev_build")

            vcs {
                root(vcsRoot)
            }

            buildInitializer.invoke(this)
        }
    }

    var deployUat: BuildType? = null

    subProject { // this project should be restricted
        name = "Pre-prod $projectName"

        id("${projectName.toId()}_preProd")

        val deployDev = buildType {
            name = "Deploy dev"

            id("${projectName.toId()}_proProd_dev")

            dependencies {
                snapshot(build!!) {}
            }
        }
        val deployTest = buildType {
            name = "Deploy test"

            id("${projectName.toId()}_proProd_test")

            dependencies {
                snapshot(deployDev) {}
            }
        }
        deployUat = buildType {
            name = "Deploy uat"

            id("${projectName.toId()}_proProd_uat")

            dependencies {
                snapshot(deployTest) {}
            }
        }
    }

    subProject { // this project should be restricted
        name = "Prod $projectName"

        id("${projectName.toId()}_prod")

        buildType {
            name = "Deploy prod"

            id("${projectName.toId()}_prod_deploy")

            dependencies {
                snapshot(deployUat!!) {}
            }
        }
    }

})