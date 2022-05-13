package com.mosect.smali.plugin

import com.mosect.smali.plugin.dex.DexHandler
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class SmaliPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.android.sourceSets.all {
            SmaliExtension extension = it.getExtensions().create('smali', SmaliExtension)
            extension.dirs.add(new File(project.projectDir, "src/${it.name}/smali"))
            extension.addOperationFile(new File(project.projectDir, "src/${it.name}/class-operation.txt"))
            extension.addPositionFile(new File(project.projectDir, "src/${it.name}/class-position.txt"))
            if (it.name == 'main') {
                extension.addOperationFile(new File(project.projectDir, 'class-operation.txt'))
                extension.addPositionFile(new File(project.projectDir, 'class-position.txt'))
            }
        }

        project.android.applicationVariants.all {
            Task task = it.packageApplication
            def variant = it
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
            it.packageApplication.doFirst {
                List<File> dexFiles = []
                task.inputs.files.each {
                    project.fileTree(it).each {
                        if (it.name ==~ '^classes([2-9][0-9]?)*\\.dex$') {
                            dexFiles.add(it)
                        }
                    }
                }
                if (!dexFiles) {
                    System.err.println('DexHandler:ignore: dexFiles not found')
                    return
                }

                File dexDir = dexFiles.get(0).parentFile
                File tempDir = new File(project.buildDir, "smali")
                project.delete(tempDir)
                DexHandler dexHandler = new DexHandler()
                dexHandler.tempDir = tempDir
                dexFiles.each {
                    println("DexHandler:addDexFile: ${it.absolutePath}")
                    dexHandler.addJavaDexFile(it)
                }

                int apiLevel = 15
                List<File> classOperationFiles = []
                List<File> classPositionFiles = []
                variant.sourceSets.each {
                    if (null != it.smali.apiLevel) {
                        apiLevel = it.smali.apiLevel
                    }

                    it.smali.operationFiles.each { File file ->
                        if (file.exists() && file.isFile()) {
                            classOperationFiles.add(file)
                        }
                    }

                    it.smali.positionFiles.each { File file ->
                        if (file.exists() && file.isFile()) {
                            classPositionFiles.add(file)
                        }
                    }

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
                    it.smali.operationFiles.each { File file ->
                        dexHandler.addOperationFile(file)
                    }
                    it.smali.positionFiles.each { File file ->
                        dexHandler.addPositionFile(file)
                    }
                }
                println("DexHandler:apiLevel ${apiLevel}")
                dexHandler.apiLevel = apiLevel
                println("DexHandler:run")
                File outDir = dexHandler.run()
                println("DexHandler:apply")
                dexFiles.each {
                    it.delete()
                }
                project.copy {
                    from(project.fileTree(outDir))
                    into(dexDir)
                }
                println("DexHandler:ok")
            }
        }
    }
}
