package demo.templates

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.Template

object MavenAksBuild2024 : Template({
    name = "MAVEN AKS BUILD 2024"

    publishArtifacts = PublishMode.SUCCESSFUL

    params {
        param("docker.java.jdk.version", "<<This parameter gets updated during runtime>>")
        param("docker.aks.artifactory.path", "docker.artifactory.platform.manulife.io")
        param("java.jdk.version", "<<This parameter gets updated during runtime>>")
    }

    steps {
        powerShell {
            name = "DOCKER LOGIN"
            scriptMode = script {
                content = """
                    ${'$'}retry_counter = 0
                    ${'$'}sleep_counter = 5
                    
                    do {
                        if ("%system.agent.name%" -like "*docker*" -or "%system.agent.name%" -like "*aks-linux*"){
                            echo "%artifactory.password%" | docker login -u %artifactory.user% --password-stdin %docker.aks.artifactory.path%
                        }
                        else {
                            docker login -u %artifactory.user% -p %artifactory.password% %docker.aks.artifactory.path%
                        }
                    
                    	#Returns true if the command above executed successfully; if false a loop will be executed until 5 tries
                        if (${'$'}?){
                            break
                        }
                        else {
                            Write-Host "Login failed! Retrying to login in ${'$'}sleep_counter seconds ... "
                            ${'$'}retry_counter++
                            start-sleep -Seconds 5
                        }
                    
                        Write-Host "Retry count ${'$'}retry_counter"
                    } while (${'$'}retry_counter -lt 5)
                """.trimIndent()
            }
            param("org.jfrog.artifactory.selectedDeployableServer.downloadSpecSource", "Job configuration")
            param("org.jfrog.artifactory.selectedDeployableServer.useSpecs", "false")
            param("org.jfrog.artifactory.selectedDeployableServer.uploadSpecSource", "Job configuration")
        }
        powerShell {
            name = "CHECK JAVA VERSION IN POM.XML"
            scriptMode = script {
                content = """
                    #${'$'}file_location = "pom.xml"
                    Write-Host "*********************************"
                    Write-Host "This step is to check the Java Version declared in pom.xml. If this step fails, please contact DevSecOps Team!"
                    Write-Host "*********************************"
                    
                    [xml]${'$'}pom = Get-Content "pom.xml"
                    
                    if (-not (Test-Path pom.xml)) {
                        Write-Host "Cannot find path 'pom.xml' because it does not exist. Please contact DevSecOps Team!"
                        exit 1
                    }
                    else {
                        ${'$'}javaVersion = ${'$'}pom.project.properties.'java.version' -replace '\..*'
                    
                        switch -wildcard (${'$'}javaVersion) {
                            "11" {
                                ${'$'}jdk_version = "%env.JDK_11_0_x64%"
                                ${'$'}docker_jdk_version = "11"
                            }
                            "17" {
                                ${'$'}jdk_version = "%env.JDK_17_0_x64%"
                                ${'$'}docker_jdk_version = "17"
                            }
                            default {
                                Write-Host "*** JDK version (${'$'}javaVersion) defined in pom.xml is unsupported. ***"
                                exit 1
                            }
                        }
                    
                        #set value for docker.java.jdk.version
                        "##teamcity[setParameter name='docker.java.jdk.version' value='${'$'}docker_jdk_version']"
                        "##teamcity[setParameter name='java.jdk.version' value='${'$'}jdk_version']"
                    
                        #Result
                        #Write-Host "JDK Version: ${'$'}jdk_version"
                        #Write-Host "Java Version (Docker): ${'$'}docker_jdk_version"
                        Write-Host "JDK Version used: ${'$'}javaVersion"
                    }
                """.trimIndent()
            }
            param("org.jfrog.artifactory.selectedDeployableServer.downloadSpecSource", "Job configuration")
            param("org.jfrog.artifactory.selectedDeployableServer.useSpecs", "false")
            param("org.jfrog.artifactory.selectedDeployableServer.uploadSpecSource", "Job configuration")
        }
        maven {
            name = "PACKAGE - OLD"
            enabled = false
            goals = "clean package"
            mavenVersion = bundled_3_6()
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
            jdkHome = "%java.jdk.version%"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
        maven {
            name = "PACKAGE"
            goals = "clean package"
            mavenVersion = bundled_3_6()
            userSettingsSelection = "settings.xml"
            localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT
            jdkHome = "%java.jdk.version%"
            dockerImage = "%docker.aks.artifactory.path%/openjdk:%docker.java.jdk.version%"
            dockerImagePlatform = MavenBuildStep.ImagePlatform.Linux
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
        powerShell {
            name = "SET TC BUILD NUMBER"
            scriptMode = script {
                content = """
                    ${'$'}mavenVersion = "%maven.project.version%" 
                    ${'$'}build_number = ${'$'}mavenVersion.replace("-SNAPSHOT","") + ".%build.counter%"
                    ${'$'}version = ${'$'}mavenVersion.replace("-SNAPSHOT","")
                    
                    Write-Output "Updating build number to ${'$'}build_number ...."
                    Write-Output "##teamcity[buildNumber '${'$'}build_number']"
                    
                    Write-Output "Updating version number to ${'$'}mavenVersion ...."
                    Write-Output "##teamcity[setParameter name='release.version' value='${'$'}version']"
                """.trimIndent()
            }
        }
    }

    requirements {
        matches("system.agent.name", "AZWAPPTCAP0[78].*", "RQ_445")
        doesNotContain("system.agent.name", "nodejs", "RQ_1969")
        doesNotContain("system.agent.name", "dotnet", "RQ_2888")
        startsWith("system.agent.name", "aks-teamcityagent-latest", "RQ_2284")
    }
    })
