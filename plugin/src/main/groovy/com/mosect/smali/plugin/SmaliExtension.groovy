package com.mosect.smali.plugin

class SmaliExtension {

    private List<File> dirs = []

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
        this.dirs.addAll(dirs)
    }

    void addDir(File dir) {
        this.dirs.add(dir)
    }
}