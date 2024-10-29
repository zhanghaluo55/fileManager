package com.aspire.assess.entity;

import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.util.Locale;

public class FileBean {
    private String fileName;
    private Boolean isFolder;
    private String fileType;
    private File file;
    private long fileSize;
    private long fileTime;
    private long fileAccessTime;
    private long fileChangeTime;
    //private final int version = android.os.Build.VERSION.SDK_INT;
    public FileBean(File file) {
        this.fileName = file.getName();
        if (file.isDirectory()){
            this.isFolder = true;
            this.fileType = "folder";
        } else {
            this.isFolder = false;
            this.fileType = getFileType(file.getName());
        }
        this.file = file;
        this.fileSize = file.length();
        this.fileTime = file.lastModified();


        try {
            this.fileAccessTime = 1000 * (Os.lstat(file.getAbsolutePath()).st_atime);
        } catch (ErrnoException e) {
            this.fileAccessTime = 0;
            e.printStackTrace();
        }

        try {
            this.fileChangeTime = 1000 * (Os.lstat(file.getAbsolutePath()).st_ctime);
        } catch (ErrnoException e) {
            this.fileChangeTime = 0;
            e.printStackTrace();
        }

    }

    private String getFileType(String fileName) {
        if (!fileName.equals("")) {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex != -1) {
                return fileName.substring(dotIndex+1).toLowerCase(Locale.US);
            }
        }
        return "";
    }

    public Boolean isFolder() {
        return isFolder;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public File getFile() {
        return file;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getFileTime() {
        return fileTime;
    }

    public long getFileAccessTime() {
        return fileAccessTime;
    }

    public long getFileChangeTime() {
        return fileChangeTime;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFolder(Boolean folder) {
        isFolder = folder;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileTime(long fileTime) {
        this.fileTime = fileTime;
    }

    public void setFileAccessTime(long fileAccessTime) {
        this.fileAccessTime = fileAccessTime;
    }

    public void setFileChangeTime(long fileChangeTime) {
        this.fileChangeTime = fileChangeTime;
    }
}
