package module.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Created by ROOT on 2017/9/20.
 */

public class ModulePlugin implements Plugin<Project> {
    private static final String MAIN_MODULE = 'mainModule'
    private static final String AS_APPLICATION = 'asApplication'
    public static final String AAR_PATH = 'aarPath'

    @Override
    public void apply(Project project) {
        println('****** Module Plugin start ******')
        def ext = project.extensions.create('moduleExt', ModuleExtension)
        def taskNames = project.gradle.startParameter.taskNames
        def projectName = project.name
        println("current module is [${projectName}]");
        System.err.println("project path=" + project.path)
        def assembleTask = assembleProjectTasks(taskNames)
        if (!project.rootProject.hasProperty(MAIN_MODULE))
            throw new RuntimeException("config [${MAIN_MODULE}] in rootProject's gradle.properties.")
        if (!project.rootProject.hasProperty(AAR_PATH))
            throw new RuntimeException("config [${AAR_PATH}] in rootProject's gradle.properties.")
        def rootMainModule = project.rootProject.property(MAIN_MODULE)
        def mainModule = rootMainModule
        if (assembleTask.isAssemble) {
            mainModule = determineMain(project, assembleTask)
            println("mainModule=${mainModule}")
        }

        final def asApplication
        if (project.hasProperty(AS_APPLICATION)) {
            asApplication = Boolean.parseBoolean(project.property(AS_APPLICATION))
        } else {
            asApplication = false
        }

        // 判断是否作为Application
        if (asApplication && assembleTask.isAssemble) {
            asApplication = projectName == mainModule || projectName == rootMainModule
        }

        if (asApplication) {
            project.apply {
                plugin: 'com.android.application'
            }
            println("apply as application. ${projectName}")
            if (projectName != rootMainModule) {
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/debug/AndroidManifest.xml'
                        java.srcDirs += 'src/main/debug/java'
                    }
                }
                println("apply release in module: [${projectName}]")
            }
            if (assembleTask.isAssemble && projectName == mainModule) {
                compileModule(project, ext, assembleTask)
            }
        } else {
            project.apply {
                plugin: 'com.android.library'
            }
            println("apply as library. ${projectName}")
            if (projectName != rootMainModule) {
                project.android.sourceSets {
                    main {
                        manifest.srcFile 'src/main/release/AndroidManifest.xml'
                    }
                }
            }
            project.afterEvaluate {
                Task assembleReleaseTask = project.tasks.findByPath('assembleRelease')
                System.err.println("task=" + assembleReleaseTask)
                if (assembleReleaseTask != null) {
                    assembleReleaseTask.doLast {
                        File inFile = project.file("build/outputs/aar/${projectName}-release.aar")
                        def String aarPath = project.rootProject.property(AAR_PATH)
                        File outFile = project.rootProject.file(aarPath)
                        File desFile = project.file("${projectName}-release.aar")
                        project.copy {
                            from inFile
                            into outFile
                            rename {
                                def fileName -> desFile.name
                            }
                        }
                        println("${projectName}-release.aar copy to " + outFile)
                    }
                }
            }
        }
        println('****** Module Plugin end ******')
    }

    def compileModule(project, ext, assembleTask) {
        def componentString
        if (assembleTask.isDebug) {
//            componentString = ext.debugComponent
            componentString = project.property('debugComponent')
        } else {
//            componentString = ext.releaseComponent
            componentString = project.property('releaseComponent')
        }
        println(" componentString=${componentString}")
        if (componentString == null || componentString.length() == 0) {
            println("no more component for project: ${project.name}")
            return
        }
        def componentSplits = componentString.split(',')
        if (componentSplits == null || componentSplits.size() == 0) {
            println("no more component for project: ${project.name}")
            return
        }
        componentSplits.each { component ->
            if (!component.contains(':')) {
                project.dependencies.add('compile', project.project(":${component}"))
                println("add dependencies [${component}]")
            } else {
                def String aarPath = project.rootProject.property(AAR_PATH)
                if (!aarPath.endsWith('/')) {
                    aarPath += '/'
                }
                println("aarPath=${aarPath}")
                def aarName = component.split(':')[1]
                def file = project.rootProject.file("${aarPath}${aarName}-release.aar")
                if (file.exists()) {
                    project.dependencies.add('compile', "${component}-release@aar")
                    println("add dependencies [${component}-release@aar]")
                } else {
                    throw new FileNotFoundException("FileNotFound ${file.name}")
                }
            }
        }

    }

    def determineMain(project, assembleTask) {
        def moduleName
        if (assembleTask.modules.size() > 0
                && assembleTask.modules[0].trim().length() > 0
                && assembleTask.modules[0] != 'all') {
            moduleName = assembleTask.modules[0]
        } else {
            moduleName = project.rootProject.property(MAIN_MODULE)
        }
        if (moduleName == null || moduleName.trim().length() == 0)
            moduleName = 'app'
        return moduleName
    }
    /**
     * task整理
     * @param taskNames
     * @return
     */
    def assembleProjectTasks(taskNames) {
        def assembleTask = new AssembleTask()
        taskNames.each { task ->
            println("task=${task}")
            if (task.toUpperCase().contains('ASSEMBLE')
                    || task == 'aR'
                    || task.toUpperCase().contains('RESGUARD')) {
                if (task.toUpperCase().contains('DEBUG'))
                    assembleTask.isDebug = true
                assembleTask.isAssemble = true
                // :app:taskName
                def splits = task.split(':')
                if (splits.size() > 1) {
                    def module = splits[0] ?: splits[1]
                    assembleTask.modules.add(module)
                } else {
                    assembleTask.modules.add('all')
                }
                println("splits=${splits.toString()}")
            }
        }
        return assembleTask
    }

    private static class AssembleTask {
        def isDebug
        def isAssemble
        def modules = []
    }

}
