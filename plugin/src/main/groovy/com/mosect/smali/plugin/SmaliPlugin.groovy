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
        }
        project.android.applicationVariants.all {
            Task task = it.packageApplication
            def variant = it
            task.doFirst {
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
                variant.sourceSets.each {
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
