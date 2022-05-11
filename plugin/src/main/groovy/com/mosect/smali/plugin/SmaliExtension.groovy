package com.mosect.smali.plugin

class SmaliExtension {

    private final List<File> dirs = []
    private final List<File> operationFiles = []
    private final List<File> positionFiles = []
    private Integer apiLevel = null

    List<File> getDirs() {
        return dirs
    }

    void setDirs(List<File> dirs) {
        this.dirs.clear()
        if (dirs) {
            this.dirs.addAll(dirs)
        }
    }

    void addDirs(File... dirs) {
        if (dirs) {
            this.dirs.addAll(dirs)
        }
    }

    void addDir(File dir) {
        this.dirs.add(dir)
    }

    List<File> getOperationFiles() {
        return this.operationFiles
    }

    void setOperationFiles(List<File> files) {
        this.operationFiles.clear()
        if (files) {
            this.operationFiles.addAll(files)
        }
    }

    void addOperationFiles(File... files) {
        if (files) {
            this.operationFiles.addAll(files)
        }
    }

    void addOperationFile(File file) {
        if (null != file) {
            this.operationFiles.add(file)
        }
    }

    List<File> getPositionFiles() {
        return this.positionFiles
    }

    void setPositionFiles(List<File> files) {
        this.positionFiles.clear()
        if (files) {
            this.positionFiles.addAll(files)
        }
    }

    void addPositionFiles(File... files) {
        if (files) {
            this.positionFiles.addAll(files)
        }
    }

    void addPositionFile(File file) {
        if (null != file) {
            this.positionFiles.add(file)
        }
    }

    void setApiLevel(Integer apiLevel) {
        this.apiLevel = apiLevel
    }

    Integer getApiLevel() {
        return this.apiLevel
    }
}