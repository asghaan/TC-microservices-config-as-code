package demo

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.maven

val javaBuild: BuildType.() -> Unit = {
    steps {
        maven {
            goals = "package"
        }
    }
}

val dotNetBuild: BuildType.() -> Unit = {
    steps {
        dotnetBuild {
            configuration = "build"
        }
    }
}
