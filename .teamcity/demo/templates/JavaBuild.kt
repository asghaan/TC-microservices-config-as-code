package demo.templates

import jetbrains.buildServer.configs.kotlin.Template
import jetbrains.buildServer.configs.kotlin.buildSteps.maven

object JavaBuild : Template({
    name = "Java Build template"

    steps {
        maven {
            goals = "package"
        }
    }

    artifactRules = "target/*"
})