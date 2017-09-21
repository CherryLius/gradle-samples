package cherry.android.jcenter.publish

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publication.maven.internal.deployer.BaseMavenInstaller
import org.gradle.api.publication.maven.internal.pom.DefaultMavenPom
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

class JcenterPublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasAndroidLib = project.plugins.withType(LibraryPlugin)
        def hasJavaLib = project.plugins.withType(JavaPlugin)
        if (!hasApp && !hasAndroidLib && !hasJavaLib)
            throw new IllegalStateException("'android' or 'android-library' or" +
                    " 'java' plugin required.")

        def jcenterExtension = project.extensions.create("jcenterPublish", JcenterPublishExtension)

        def isAndroid = project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin)

        if (isAndroid) {
            project.plugins.apply('com.github.dcendents.android-maven')
        } else {
            project.plugins.apply('maven')
        }
        project.plugins.apply('com.jfrog.bintray')

        project.tasks.withType(Javadoc.class) {
            options {
                encoding 'UTF-8'
                charSet 'UTF-8'
                links 'http://docs.oracle.com/javase/7/docs/api'
            }
        }
        project.tasks.getByName('install') {
            project.group = jcenterExtension.group
            project.version = jcenterExtension.version
            BaseMavenInstaller installer = repositories.mavenInstaller
            DefaultMavenPom pom = installer.getPom();
            def pomProject = pom.getMavenProject()
            pomProject.packaging = 'aar'
            pomProject.name = jcenterExtension.description
            pomProject.url = jcenterExtension.siteUrl

//            pomProject.addLicense(new License(name: 'The Apache Software License, Version 2.0',
//                    url: 'http://www.apache.org/licenses/LICENSE-2.0.txt'))
//
//            pomProject.addDeveloper(new Developer(id: 'cherry', name: 'cherry', email: '767041809@qq.com'))

//            pomProject.setScm(new Scm(connection: jcenterExtension.gitUrl,
//                    developerConnection: jcenterExtension.gitUrl,
//                    url: jcenterExtension.siteUrl))
//            }
        }

        project.tasks.create('sourcesJar', Jar.class)
        if (isAndroid) {
            project.sourcesJar.from(project.android.sourceSets.main.java.srcDirs)
        } else {
            project.sourcesJar.from(project.sourceSets.main.java.srcDirs)
        }
        project.sourcesJar.classifier = 'sources'

        if (isAndroid) {
            project.tasks.create('javadoc', Javadoc.class)
            project.javadoc.source = project.android.sourceSets.main.java.srcDirs
            project.javadoc.classpath += project.files(project.android.getBootClasspath().join(File.pathSeparator))
        }

        project.tasks.create('javadocJar', Jar.class)
        project.javadocJar.from(project.javadoc.destinationDir)
        project.javadocJar.classifier = 'javadoc'


        project.javadocJar.dependsOn project.javadoc

        project.artifacts {
            archives project.javadocJar
            archives project.sourcesJar
        }

        Properties properties = new Properties()
        properties.load(project.rootProject.file('local.properties').newDataInputStream())

        project.bintray {
            project.group = jcenterExtension.group
            project.version = jcenterExtension.version
            user = properties.getProperty('bintray.user')
            key = properties.getProperty('bintray.apikey')
            configurations = ['archives']
            println(" pkg = " + pkg + '\n' + jcenterExtension.name)
            //BintrayExtension.PackageConfig
            pkg {
                repo = 'Maven'
                name = jcenterExtension.name
                websiteUrl = jcenterExtension.siteUrl
                vcsUrl = jcenterExtension.gitUrl
                licenses = ["Apache-2.0"]
                publish = true
            }
        }

        project.tasks.create("showArgs") {
            doLast {
                println("1------name=${extension.name}")
            }
        }
    }
}