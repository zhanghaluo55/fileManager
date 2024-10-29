package com.aspire.assess.utils;

import android.os.Environment;

import com.aspire.assess.entity.FileBean;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GetFilesUtils {
    public static final String SORT_BY_TIME = "SORT_BY_TIME";
    public static final String SORT_BY_SIZE = "SORT_BY_SIZE";
    public static final String SORT_BY_DEFAULT = "SORT_BY_DEFAULT";

    private static GetFilesUtils instance;

    private GetFilesUtils() {
    }

    public static synchronized GetFilesUtils getInstance() {
        if (instance == null) {
            instance = new GetFilesUtils();
        }
        return instance;
    }

    /**
     * @param file 文件目录树根节点
     * @return
     */
    public List<FileBean> getChildNode(File file) {
        List<FileBean> list = new ArrayList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childNode: files) {
                    if (childNode.getName().startsWith(".")) {
                        continue;
                    }
                    FileBean fileBean = new FileBean(childNode);
                    list.add(fileBean);
                }
            }
        }
        return list;
    }

    /**
     *
     * @param path 文件目录树根节点路径
     * @return
     */
    public List<FileBean> getChildNode(String path) {
        File file = new File(path);
        return getChildNode(file);
    }

    //获取SD卡路径
    public String getSDPath() {
        String sdCard = Environment.getExternalStorageState();
        if (sdCard.equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            return null;
        }
    }

    //获取文件目录树根路径，若sd卡已挂载则返回sd卡路径，否则返回程序存储路径的data
    public String getBasePath() {
        String basePath = getSDPath();
        if (basePath == null) {
            return Environment.getDataDirectory().getAbsolutePath();
        } else {
            return basePath;
        }
    }

    /**
     * 文件排序方式：按文件夹优先、大小升序、时间升序
     * @param pattern
     * @return
     */
    public Comparator<FileBean> fileOrder(final String pattern) {
        return (o1, o2) -> {
            int order = 0;
            long diff;
            switch (pattern) {
                case SORT_BY_SIZE:
                    diff = o1.getFileSize() - o2.getFileSize();
                    if (diff != 0) {
                        order = diff > 0 ? 1 : -1;
                    }
                    break;
                case SORT_BY_TIME:
                    diff = o1.getFileTime() - o2.getFileTime();
                    if (diff != 0) {
                        order = diff > 0 ? 1 : -1;
                    }
                    break;
                case SORT_BY_DEFAULT:
                default:
                    if (o1.isFolder() && !o2.isFolder()) {
                        order = -1;
                    } else if (!o1.isFolder() && o2.isFolder()) {
                        order = 1;
                    } else {
                        order = o1.getFileName().compareTo(o2.getFileName());
                    }
                    break;
            }
            return order;
        };
    }

    public Comparator<FileBean> defaultOrder() {
        return fileOrder(SORT_BY_DEFAULT);
    }

    public String getFileSizeStr(long fileSize) {
        String res;
        DecimalFormat df = new DecimalFormat("#.00");
        if (fileSize < 1024) {
            res = fileSize + "B";
        } else if (fileSize < 1048576) {
            res = df.format(fileSize / (double) 1024) + "KB";
        } else if (fileSize < 1073741824) {
            res = df.format(fileSize / (double) 1048576) + "MB";
        } else {
            res = df.format(fileSize / (double) 1073741824) + "GB";
        }
        return res;
    }

}
