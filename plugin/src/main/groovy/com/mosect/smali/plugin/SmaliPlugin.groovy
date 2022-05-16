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

        project.android.applicationVariants.all { variant ->
            List<Task> dexTasks = []
            project.tasks.each {
                def vn = it.properties.get('variantName')
                if (vn == variant.name) {
                    // variant task
                    def dexTask = it.name ==~ '^transformDex.*Merger.*$' ||
                            it.name ==~ '^minify.+WithR8.*$' ||
                            it.name ==~ '^transformClasses.+WithR8.*$' ||
                            it.name ==~ '^mergeDex.*$'
                    if (dexTask) {
                        // dex task
                        dexTasks.add(it)
                    }
                }
            }
            dexTasks.each {
                DexHandler dexHandler = null
                File dexDir = null
                List<File> originalDexFiles = []

                // create smali task
                Task smaliTask = project.tasks.create("${it.name}WithSmali")
                smaliTask.setGroup('smali')
                // set task outputs
                File tempDir = new File(project.buildDir, "smali/${smaliTask.name}")
                smaliTask.outputs.dir(tempDir)
                // set task inputs
                variant.sourceSets.each {
                    SmaliExtension smaliExtension = it.smali
                    smaliTask.inputs.files(smaliExtension.positionFiles)
                    smaliTask.inputs.files(smaliExtension.operationFiles)
                    smaliExtension.dirs.each {
                        if (it.isDirectory()) {
                            smaliTask.inputs.dir(it)
                        }
                    }
                }
                it.outputs.files.each {
                    if (it.isDirectory()) {
                        smaliTask.inputs.dir(it)
                    } else {
                        smaliTask.inputs.files(it)
                    }
                }
                smaliTask.doLast {
                    if (null != dexHandler) {
                        println("DexHandler:run")
                        File outDir = dexHandler.run()
                        println("DexHandler:apply")
                        // delete original dex
                        originalDexFiles.each {
                            it.delete()
                        }
                        project.copy {
                            from(outDir)
                            into(dexDir)
                        }
                        println("DexHandler:ok")
                    }
                }

                // run smali task after dex task
                it.finalizedBy(smaliTask)

                // add ext operate to dex task
                it.doLast {
                    // find dex files
                    List<File> dexFiles = []
                    outputs.files.each {
                        project.fileTree(it).each {
                            if (it.name ==~ '^classes([0-9]{1,2})?\\.dex$') {
                                dexFiles.add(it)
                            }
                        }
                    }
                    if (dexFiles.size() > 0) {
                        println("DexHandler:configure")
                        // exists dex file
                        dexHandler = new DexHandler()
                        dexHandler.tempDir = tempDir
                        dexFiles.each {
                            println("DexHandler:addDexFile: ${it.absolutePath}")
                            originalDexFiles.add(it)
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
                                    dexHandler.addOperationFile(file)
                                }
                            }

                            it.smali.positionFiles.each { File file ->
                                if (file.exists() && file.isFile()) {
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
                        dexDir = dexFiles[0].parentFile
                        // find all classes
                        HashSet<String> classes = new HashSet<>()
                        int maxClassesIndex = 1
                        dexHandler.allOriginalSourceClasses().each {
                            if (it == 1) {
                                classes.add('classes.dex')
                            } else {
                                classes.add("classes${it}.dex")
                            }
                            if (it > maxClassesIndex) maxClassesIndex = it
                        }
                        classes.add('classes.dex')
                        classes.add("classes${maxClassesIndex + 1}.dex")
                        // create classes if not exists
                        classes.each {
                            // create empty file
                            File file = new File(dexDir, it)
                            if (!file.exists()) {
                                println("DexHandler:createEmptyDex ${file.getAbsolutePath()}")
                                file.text = ''
                                originalDexFiles.add(file)
                            }
                        }
                    }
                }
            }
        }
    }
}
