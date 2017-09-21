package cherry.extension.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExtensionPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create("dateAndTime", DateTimeExtension)
        println("timeFormat=${extension.timeFormat} dataFormat=${extension.dateFormat}")
        project.task('showTime') << {
            println("2 timeFormat=${extension.timeFormat} dataFormat=${extension.dateFormat}")
            println "Current time is " + new Date().format(extension.timeFormat)
        }
        project.tasks.create('showDate') << {
            println "Current date is " + new Date().format(extension.dateFormat)
        }
    }
}