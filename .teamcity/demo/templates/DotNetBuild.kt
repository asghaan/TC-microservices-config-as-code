package demo.templates

import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetBuild

object DotNetBuild : Template({
    name = "DotNetBuild"

    steps {
        dotnetBuild {
            configuration = "build"
        }
    }

    artifactRules = "target/*"
})