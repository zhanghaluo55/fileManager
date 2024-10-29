package com.aspire.assess.adapter;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aspire.assess.R;
import com.aspire.assess.activity.BaseActivity;
import com.aspire.assess.activity.MainActivity;
import com.aspire.assess.entity.FileBean;
import com.aspire.assess.utils.GetFilesUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FileViewAdapter extends RecyclerView.Adapter<FileViewAdapter.ViewHolder> {
    //是否显示后缀名
    private boolean showExt;
    //多模式标志位
    private boolean selectMode;
    private final List<FileBean> fileList;
    //选择文件列表
    private final Set<FileBean> selectSet = new HashSet<>();
    //选择状态列表
    private final SparseBooleanArray checkStates = new SparseBooleanArray();
    private final Map<String, Integer> fileTypeIconMap = new HashMap<>();
    private BaseActivity mContext = null;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View fileView;
        ImageView fileImage;
        TextView fileName;
        TextView fileModifiedTime;
        TextView fileAccessTime;
        TextView fileSize;
        CheckBox selected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileView = itemView;
            selected = itemView.findViewById(R.id.selected);
            fileImage = itemView.findViewById(R.id.file_image);
            fileName = itemView.findViewById(R.id.file_name);
            fileModifiedTime = itemView.findViewById(R.id.file_modified_time);
            fileAccessTime = itemView.findViewById(R.id.file_access_time);
            fileSize = itemView.findViewById(R.id.file_size);
        }
    }


    public FileViewAdapter(List<FileBean> fileList) {
        Collections.sort(fileList, GetFilesUtils.getInstance().defaultOrder());
        this.fileList = fileList;
        fileTypeIconMap.put("unknown", R.drawable.filetype_unknow);
        fileTypeIconMap.put("folder", R.drawable.filetype_folder);
        fileTypeIconMap.put("txt", R.drawable.filetype_txt);
        fileTypeIconMap.put("pdf", R.drawable.filetype_pdf);
        fileTypeIconMap.put("mp3", R.drawable.filetype_mp3);
        fileTypeIconMap.put("mp4", R.drawable.filetype_mp4);
        fileTypeIconMap.put("jpg", R.drawable.filetype_jpg);
        fileTypeIconMap.put("png", R.drawable.filetype_png);
    }

    public Set<FileBean> getSelectSet() {
        return selectSet;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = (BaseActivity) parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_view_item, parent, false);
        return new ViewHolder(view);
    }
    //ViewHolder绑定
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileBean fileBean = fileList.get(position);
        //实体类 ui数据绑定
        Integer fileImage = fileTypeIconMap.get(fileBean.getFileType());
        if (fileImage == null) {
            fileImage = fileTypeIconMap.get("unknown");
        }

        holder.fileImage.setImageResource(fileImage);
        String fileName = fileBean.getFileName();
        if (!showExt && !fileBean.isFolder() && fileName.indexOf(".") > 0) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        holder.fileName.setText(fileName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        holder.fileModifiedTime.setText(sdf.format(new Date(fileBean.getFileTime())));
        holder.fileAccessTime.setText(sdf.format(new Date(fileBean.getFileAccessTime())));
        holder.fileSize.setText(GetFilesUtils.getInstance().getFileSizeStr(fileBean.getFileSize()));

        //复选框初始化
        holder.selected.setTag(position);
        if (selectMode) {
            holder.selected.setVisibility(View.VISIBLE);
            holder.selected.setChecked(checkStates.get(position, false));
        } else {
            holder.selected.setVisibility(View.GONE);
            holder.selected.setChecked(false);
            checkStates.clear();
        }

        //复选框点击事件
        holder.selected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = (int) buttonView.getTag();
            FileBean file = fileList.get(pos);
            if (isChecked) {
                checkStates.put(pos, true);
                selectSet.add(file);
            } else {
                checkStates.delete(pos);
                selectSet.remove(file);
            }
        });

        holder.fileView.setTag(position);
        holder.fileView.setOnClickListener(v -> {
            FileBean file = fileList.get(position);
            if (selectMode) {
                holder.selected.setChecked(!holder.selected.isChecked());
                return;
            }
            if (file.isFolder()) {
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                intent.putExtra("path", file.getFile().toString());
                v.getContext().startActivity(intent);
            } else {
                boolean isNeedMatch = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
                StrictMode.VmPolicy defaultVmPolicy = null;

                try {
                    if (isNeedMatch) {
                        defaultVmPolicy = StrictMode.getVmPolicy();
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                    }

                    //根据文件的mimeType判断文件类型, 并打开对应程序
                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                    String mime = mimeTypeMap.getMimeTypeFromExtension(file.getFileType());
                    mime = TextUtils.isEmpty(mime) ? "" : mime;
                    Intent intent = new Intent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(file.getFile().toString())), mime);
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(v.getContext(), "无法打开", Toast.LENGTH_SHORT).show();
                } finally {
                    //重置权限
                    if (isNeedMatch) {
                        StrictMode.setVmPolicy(defaultVmPolicy);
                    }
                }

            }
        });

        //长按进入多选模式
        holder.fileView.setOnLongClickListener(view -> {
            if (!selectMode) {
                checkStates.put(position, true);
                selectSet.add(fileList.get(position));
                goSelectMode();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return this.fileList.size();
    }

    //打开多选模式
    public void goSelectMode() {
        selectMode = true;
        mContext.setSelectModeShow(true);
        notifyDataSetChanged();
    }

    //关闭多选模式
    public boolean leaveSelectMode() {
        if (!selectMode) {
            return false;
        }
        selectMode = false;
        mContext.setSelectModeShow(false);
        selectSet.clear();
        checkStates.clear();
        notifyDataSetChanged();
        return true;
    }

    //搜索文件，将搜索到的文件加入到列表
    public void addFile(File file) {
        if (fileList.add(new FileBean(file))) {
            notifyItemChanged(fileList.size() - 1);
        }
    }

    //清空搜索列表
    public void clearFiles() {
        fileList.clear();
        notifyDataSetChanged();
    }

    //显示文件后缀名
    public void setShowExt(boolean showExt) {
        this.showExt = showExt;
        notifyDataSetChanged();
    }

    //操作剪切板后统一操作
    public void notifyOperateFinish() {
        leaveSelectMode();
        notifyDataSetChanged();
    }

    //全选
    public void selectAll() {
        for (FileBean fileBean : fileList) {
            checkStates.put(fileList.indexOf(fileBean), true);
            selectSet.add(fileBean);
        }
        goSelectMode();
    }
}
