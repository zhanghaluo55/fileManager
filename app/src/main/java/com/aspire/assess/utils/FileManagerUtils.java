package com.aspire.assess.utils;


import android.os.AsyncTask;

import com.aspire.assess.entity.FileBean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManagerUtils {
    public static final FileManagerUtils instance = new FileManagerUtils();

    private FileManagerUtils() {
    }

    public interface SearchFoundFile {
        void onFoundFile(File file);
    }

    //剪切或复制文件剪切板
    private List<FileBean> paths;
    //剪切或复制标记位
    private boolean doCut;

    /**
     * 剪切文件
     * @param paths
     */
    public void cut(List<FileBean> paths) {
        this.paths = paths;
        doCut = true;
    }

    /**
     * 复制文件
     * @param paths
     */
    public void copy(List<FileBean> paths) {
        this.paths = paths;
        doCut = false;
    }

    /**
     * 粘贴文件
     * @param target
     * @return
     * @throws IOException
     */
    public List<FileBean> paste(final File target) throws IOException {
        List<FileBean> result = new ArrayList<>();
        for (FileBean file : paths) {
            String filePath = file.getFile().getPath();
            String targetPath = target.getPath();
            if (doCut) {
                //如果目标目录非当前，则粘贴
                if (!filePath.substring(0, filePath.indexOf(file.getFileName())-1).equals(targetPath)) {
                    FileUtils.moveToDirectory(file.getFile(), target, true);
                }
            } else {
                FileUtils.copyToDirectory(file.getFile(), target);
            }
            FileBean fileBean = new FileBean(new File(target + "/" + file.getFileName()));
            result.add(fileBean);
        }
        this.paths = null;
        return result;
    }

    /**
     * 删除文件
     * @param file
     * @return
     */
    public boolean delete(File file) {
        return FileUtils.deleteQuietly(file);
    }

    /**
     * 搜索文件
     * @param asyncTask
     * @param currentPath
     * @param keyword
     * @param searchFoundFile
     */
    public void searchFiles(AsyncTask asyncTask, File currentPath, final String keyword, SearchFoundFile searchFoundFile) {
        if (asyncTask.isCancelled()) {
            return;
        }

        File[] files = currentPath.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            return;
        }

        for (File file : files) {
            if (asyncTask.isCancelled()) {
                return;
            }
            if (file.getName().equals(keyword)) {
                searchFoundFile.onFoundFile(file);
            }
            if (file.isDirectory()) {
                searchFiles(asyncTask, file, keyword, searchFoundFile);
            }
        }
    }

    /**
     * 创建文件
     * @param filePath
     * @return
     * @throws IOException
     */
    public boolean createFile(String filePath) throws IOException {
        File file = new File(filePath);
        return file.createNewFile();
    }

    /**
     * 创建文件夹
     * @param dirPath
     * @return
     */
    public boolean createDirectory(String dirPath) {
        File dir = new File(dirPath);
        return dir.mkdir();
    }

    /**
     * 移动文件或文件夹到新目录
     * @param file
     * @param dir
     * @throws IOException
     */
    public void moveToFolder(File file, File dir) throws IOException {
        if (file.isDirectory()) {
            FileUtils.moveDirectoryToDirectory(file, dir, false);
        } else {
            FileUtils.moveFileToDirectory(file, dir, false);
        }
    }

    /**
     * 合并文件
     * @param file1
     * @param file2
     * @param newFile
     * @throws IOException
     */
    public void mergeIntoFolder(File file1, File file2, File newFile) throws IOException {
        FileUtils.moveFileToDirectory(file1, newFile, true);
        FileUtils.moveFileToDirectory(file2, newFile, false);
    }

    /**
     * 判断剪切板是否为空
     * @return
     */
    public boolean isClipBoardEmpty() {
        if (this.paths == null) {
            return true;
        } else {
            return this.paths.isEmpty();
        }
    }
}
