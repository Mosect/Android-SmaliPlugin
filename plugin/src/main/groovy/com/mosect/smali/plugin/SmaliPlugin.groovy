package com.mosect.smali.plugin

import com.mosect.smali.plugin.dex.DexHandler
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class SmaliPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.android.sourceSets.all {
            // create smali extension for sourceSet
            SmaliExtension extension = it.getExtensions().create('smali', SmaliExtension)
            extension.dirs.add(new File(project.projectDir, "src/${it.name}/smali"))
            extension.addOperationFile(new File(project.projectDir, "src/${it.name}/class-operation.txt"))
            extension.addPositionFile(new File(project.projectDir, "src/${it.name}/class-position.txt"))
            if (it.name == 'main') {
                extension.addOperationFile(new File(project.projectDir, 'class-operation.txt'))
                extension.addPositionFile(new File(project.projectDir, 'class-position.txt'))
            }
        }

        project.afterEvaluate {
            project.android.applicationVariants.all { variant ->
                // find dex task
                List<Task> dexTasks = []
                project.tasks.each {
                    def vn = it.properties.get('variantName')
                    if (vn == variant.name) {
                        // variant task
                        def dexTask = it.name ==~ '^transformDex.*Merger.*$' ||
                                it.name ==~ '^minify.+WithR8.*$' ||
                                it.name ==~ '^transformClasses.+WithR8.*$' ||
                                it.name ==~ '^mergeDex.*$' ||
                                it.name ==~ '^mergeProjectDex.*$'
                        if (dexTask) {
                            // dex task
                            dexTasks.add(it)
                        }
                    }
                }
                dexTasks.each { task ->
                    // set task outputs
                    File tempDir = new File(project.buildDir, "smali/${task.name}")
                    task.outputs.dir(tempDir)
                    // set task inputs
                    variant.sourceSets.each {
                        SmaliExtension smaliExtension = it.smali
                        task.inputs.files(smaliExtension.positionFiles)
                        task.inputs.files(smaliExtension.operationFiles)
                        smaliExtension.dirs.each {
                            if (it.isDirectory()) {
                                task.inputs.dir(it)
                            }
                        }
                    }
                    task.doLast {
                        // find dex files
                        List<File> dexFiles = []
                        outputs.files.each {
                            project.fileTree(it).each {
                                if (it.name ==~ '^classes([0-9]{1,2})?\\.dex$') {
                                    dexFiles.add(it)
                                }
                            }
                        }
                        if (dexFiles.isEmpty()) {
                            System.err.println("DexHandler:skip dex file not found")
                            return
                        }

                        // exists dex file
                        DexHandler dexHandler = new DexHandler()
                        dexHandler.tempDir = tempDir
                        dexFiles.each {
                            println("DexHandler:addDexFile: ${it.absolutePath}")
                            dexHandler.addJavaDexFile(it)
                        }
                        // configure dexHandler
                        int apiLevel = 15
                        variant.sourceSets.each {
                            if (null != it.smali.apiLevel) {
                                apiLevel = it.smali.apiLevel
                            }

                            it.smali.operationFiles.each { File file ->
                                if (file.exists() && file.isFile()) {
                                    println("DexHandler:addOperationFile: ${file.absolutePath}")
                                    dexHandler.addOperationFile(file)
                                }
                            }

                            it.smali.positionFiles.each { File file ->
                                if (file.exists() && file.isFile()) {
                                    println("DexHandler:addPositionFile: ${file.absolutePath}")
                                    dexHandler.addPositionFile(file)
                                }
                            }

                            // find smali classes directory
                            it.smali.dirs.each { File dir ->
                                def dirs = dir.listFiles(new FileFilter() {
                                    @Override
                                    boolean accept(File file) {
                                        return file.isDirectory() && file.getName().matches('^classes([2-9][0-9]?)*$')
                                    }
                                })
                                if (dirs) {
                                    dirs.each {
                                        int dexIndex
                                        if (it.name == 'classes') {
                                            dexIndex = 1
                                        } else {
                                            dexIndex = Integer.parseInt(it.name.substring('classes'.length()))
                                        }
                                        println("DexHandler:addSmaliDir: ${it.absolutePath}")
                                        dexHandler.addOriginalSourceDir(dexIndex, it)
                                    }
                                }
                            }
                        }
                        println("DexHandler:apiLevel ${apiLevel}")
                        dexHandler.apiLevel = apiLevel
                        File dexDir = dexFiles[0].parentFile
                        println("DexHandler:run")
                        File outDir = dexHandler.run()
                        println("DexHandler:apply")
                        // delete original dex
                        dexFiles.each {
                            it.delete()
                        }
                        project.copy {
                            from(outDir)
                            into(dexDir)
                        }
                        println("DexHandler:ok")
                    }
                }
            }
        }
    }
}
