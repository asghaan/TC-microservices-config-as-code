import demo.ServiceProjectByCode
import demo.ServiceProjectByTemplate
import demo.dotNetBuild
import demo.templates.DotNetBuild
import demo.templates.JavaBuild
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.03"

project {
    template(JavaBuild)
    template(DotNetBuild)

    subProject(ServiceProjectByTemplate("Java Service", "First service URL", JavaBuild))

    subProject(ServiceProjectByCode("DotNet Service", "Second service URL", dotNetBuild))
}
