package com.aspire.assess.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aspire.assess.R;
import com.aspire.assess.adapter.FileViewAdapter;
import com.aspire.assess.entity.FileBean;
import com.aspire.assess.utils.DensityUtils;
import com.aspire.assess.utils.FileManagerUtils;
import com.aspire.assess.utils.GetFilesUtils;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class BaseActivity extends AppCompatActivity {
    //浮标按钮
    private FloatingActionMenu fab;
    //灰色遮罩层
    private LinearLayout greyCover;
    public RecyclerView recyclerView;
    public LinearLayout bottomSheetLayout;
    //底部操作栏
    public BottomSheetBehavior bottomSheetBehavior;
    //获取权限
    private final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    //已同意权限
    private final List<String> mPermissionList = new ArrayList<>();
    //当前文件路径树
    protected String path;
    //文件列表
    public List<FileBean> fileList;

    public FileViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutResourceId());
        //开启toolbar功能
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //文件列表视图初始化
        recyclerView = findViewById(R.id.file_container);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //底部操作初始化
        bottomSheetLayout = findViewById(R.id.bottomSheetLayout);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);

        //检查权限，若权限同意则执行init方法
        checkPermission();
        //设置底部操作栏各个按钮的点击回调
        setBottomSheet();
        //设置右下角浮标点击事件
        setFloatingMenu();
    }
    public void setBottomSheet() {
        findViewById(R.id.bottom_delete).setOnClickListener(view -> deleteFile());
        findViewById(R.id.bottom_cut).setOnClickListener(view -> cutOrCopyFile(true));
        findViewById(R.id.bottom_copy).setOnClickListener(view -> cutOrCopyFile(false));
        findViewById(R.id.bottom_paste).setOnClickListener(view -> pasteFile());
        findViewById(R.id.bottom_info).setOnClickListener(view -> infoFile());
    }

    private void setFloatingMenu() {
        fab = findViewById(R.id.fab);
        greyCover = findViewById(R.id.grey_cover);

        fab.setOnMenuButtonClickListener(view -> {
            setSelectModeShow(false);
            if (fab.isOpened()) {
                greyCover.setVisibility(View.GONE);
            } else {
                greyCover.setVisibility(View.VISIBLE);
            }
            fab.toggle(true);
        });

        FloatingActionButton createDirBtn = findViewById(R.id.create_dir);
        FloatingActionButton createFileBtn = findViewById(R.id.create_file);
        createDirBtn.setOnClickListener(view -> createFileOrDirBtnCallBack(true));
        createFileBtn.setOnClickListener(view -> createFileOrDirBtnCallBack(false));

        //  点击灰色蒙层时关闭 FloatingActionMenu
        greyCover.setOnClickListener(view -> closeFloatingMenu());
    }

    protected abstract int getLayoutResourceId();

    protected void init() {
        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        //如果path为null, 则为根节点
        if (path == null) {
            path = GetFilesUtils.getInstance().getBasePath();
        }

        // bfs遍历一层根节点渲染
        this.fileList = GetFilesUtils.getInstance().getChildNode(path);
        adapter = new FileViewAdapter(this.fileList);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 权限检验
     */
    public void checkPermission() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        }

        if (!mPermissionList.isEmpty()) {
            String[] permissions1 = mPermissionList.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permissions1, 1);
        } else {
            init();
        }
    }

    /**
     * 展示多选模式的UI
     * @param isSelectMode
     */
    public void setSelectModeShow(boolean isSelectMode) {
        if (isSelectMode) {
            fab.setVisibility(View.GONE);
            recyclerView.setPadding(0, 0, 0, DensityUtils.dip2px(this, 48));
            bottomSheetLayout.setVisibility(View.VISIBLE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            fab.setVisibility(View.VISIBLE);
            recyclerView.setPadding(0, 0, 0, 0);
            bottomSheetLayout.setVisibility(View.GONE);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * 删除文件前弹窗确认
     */
    private void deleteFile() {
        final Set<FileBean> selected = adapter.getSelectSet();
        final Context context = this;

        new AlertDialog.Builder(this)
                .setTitle("删除文件")
                .setMessage("你确定要删除" + selected.size() + "个文件/文件夹？ 此操作无法撤销。")
                .setNegativeButton("确定", (dialogInterface, i)->{
                    for (FileBean fileBean : selected) {
                        if (!FileManagerUtils.instance.delete(fileBean.getFile())) {
                            Toast.makeText(context, "删除文件失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        fileList.remove(fileBean);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(context, "删除文件成功", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("取消", (dialogInterface, i) -> {
                }).show();
    }

    /**
     * 剪切复制文件，剪切则从ui删除
     */
    private void cutOrCopyFile(boolean isCut) {
        Set<FileBean> selected = adapter.getSelectSet();
        List<FileBean> fileList = new ArrayList<>(selected);
        if (isCut) {
            FileManagerUtils.instance.cut(fileList);
            Toast.makeText(this, "已经剪切, 请移动到目标目录下粘贴", Toast.LENGTH_SHORT).show();
            this.fileList.removeAll(fileList);
        } else {
            FileManagerUtils.instance.copy(fileList);
            Toast.makeText(this,"已经复制, 请移动到目标目录下粘贴", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyOperateFinish();
    }

    /**
     * 粘贴文件
     */
    public void pasteFile() {
        if (FileManagerUtils.instance.isClipBoardEmpty()) {
            Toast.makeText(this, "剪切板为空", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            List<FileBean> result = FileManagerUtils.instance.paste(new File(path));
            fileList.addAll(result);
            Toast.makeText(this, "粘贴成功", Toast.LENGTH_SHORT).show();
            adapter.notifyOperateFinish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查看文件信息
     */
    private void infoFile() {
        Set<FileBean> selected = adapter.getSelectSet();
        List<FileBean> fileList = new ArrayList<>(selected);
        FileBean fileBean = fileList.get(0);
        if (fileList.size() > 1) {
            return;
        }

        final View dialogView = LayoutInflater.from(BaseActivity.this)
                .inflate(R.layout.input_dialog, null);
        ((TextView)dialogView.findViewById(R.id.dialog_tip)).setVisibility(View.GONE);

        final EditText editText = dialogView.findViewById(R.id.dialog_input);
        editText.setVisibility(View.GONE);

        final View fileInfoView = dialogView.findViewById(R.id.dialog_fileInfo);
        fileInfoView.setVisibility(View.VISIBLE);
        //editText.requestFocus();
        final TextView fileNameView = dialogView.findViewById(R.id.dialog_fileNameValue);
        final TextView fileTimeView = dialogView.findViewById(R.id.dialog_fileTimeValue);
        final Button fileTimeChangeBtn = dialogView.findViewById(R.id.dialog_changeFileTimeBtn);
        fileTimeChangeBtn.setVisibility(View.VISIBLE);

        final TextView fileAccessTimeView = dialogView.findViewById(R.id.dialog_fileAccessTimeValue);
        final TextView fileChangeTimeView = dialogView.findViewById(R.id.dialog_fileChangeTimeValue);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
        System.out.println("accessTime:" + fileBean.getFileAccessTime());
        System.out.println("changeTime:" + fileBean.getFileChangeTime());
        fileNameView.setText(fileBean.getFileName());
        fileTimeView.setText(sdf.format(fileBean.getFileTime()));
        fileAccessTimeView.setText(sdf.format(fileBean.getFileAccessTime()));
        fileChangeTimeView.setText(sdf.format(fileBean.getFileChangeTime()));

        final AlertDialog dialog = new AlertDialog.Builder(BaseActivity.this)
                .setTitle("文件详情").setView(dialogView)
                .create();

        fileTimeChangeBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                long time = new Date().getTime();
                fileBean.getFile().setLastModified(time);
                fileBean.setFileTime(time);
                fileTimeView.setText(sdf.format(time));
                fileAccessTimeView.setText(sdf.format(fileBean.getFileAccessTime()));
                fileChangeTimeView.setText(sdf.format(time));
                fileBean.setFileChangeTime(time);
            }
        });

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorAccent));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);


    }
    /**
     * 文件创建弹出窗口
     */
    private void createFileOrDirBtnCallBack(boolean isDirectory) {
        final View dialogView = LayoutInflater.from(BaseActivity.this)
                .inflate(R.layout.input_dialog, null);
        ((TextView)dialogView.findViewById(R.id.dialog_tip)).setText(isDirectory ? "请输入新建文件夹名称" : "请输入新建文件名");

        final EditText editText = dialogView.findViewById(R.id.dialog_input);
        editText.requestFocus();

        final AlertDialog dialog = new AlertDialog.Builder(BaseActivity.this)
                .setTitle(isDirectory ? "创建一个文件夹" : "创建一个空白文件").setView(dialogView)
                .setPositiveButton("确定", null).setNegativeButton("取消", (dialog1, which) -> {}).create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positionBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positionBtn.setOnClickListener(v -> {
                String name = editText.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(BaseActivity.this, "名称不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    String newPath = path + "/" + name;
                    createFileOrDir(newPath, isDirectory);
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorAccent));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    /**
     * 创建文件或者文件夹
     */
    public void createFileOrDir(String path, boolean isDirectory) {
        closeFloatingMenu();
        if (isDirectory) {
            boolean result = FileManagerUtils.instance.createDirectory(path);
            if (result) {
                Toast.makeText(this, "创建成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "创建失败, 可能存在同名文件夹", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            try {
                boolean result = FileManagerUtils.instance.createFile(path);
                if (result) {
                    Toast.makeText(this, "创建成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "创建失败, 可能存在同名文件", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(BaseActivity.this, "创建失败, 原因是：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            //创建成功，更新ui，下滑到新创建的文件位置
            FileBean fileBean = new FileBean(new File(path));
            fileList.add(fileBean);
            adapter.notifyOperateFinish();

            int position = fileList.indexOf(fileBean);
            if (position != -1) {
                recyclerView.scrollToPosition(position);
                LinearLayoutManager mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                mLayoutManager.scrollToPositionWithOffset(position, 0);
            }
        }
    }

    /**
     * 设置文件排序
     */
    private void setFileBeanSort(String sort){
        Collections.sort(this.fileList, GetFilesUtils.getInstance().fileOrder(sort));
        adapter.notifyDataSetChanged();
    }


    /**
     * 关闭下方操作菜单
     */
    public void closeFloatingMenu() {
        fab.close(true);
        greyCover.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                finishAfterTransition();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.show_ext:
                item.setChecked(!item.isChecked());
                adapter.setShowExt(item.isChecked());
                return true;
            case android.R.id.home:
                if (!adapter.leaveSelectMode()) {
                    if (fab.isOpened()) {
                        closeOptionsMenu();
                    } else {
                        finishAfterTransition();
                    }
                }
                return true;
            case R.id.sort_by_default:
                if (!item.isChecked()) {
                    setFileBeanSort(GetFilesUtils.SORT_BY_DEFAULT);
                    item.setChecked(true);
                }
                return true;
            case R.id.sort_by_size:
                if (!item.isChecked()) {
                    setFileBeanSort(GetFilesUtils.SORT_BY_SIZE);
                    item.setChecked(true);
                }
                return true;
            case R.id.sort_by_time:
                if (!item.isChecked()) {
                    setFileBeanSort(GetFilesUtils.SORT_BY_TIME);
                    item.setChecked(true);
                }
                return true;
            case R.id.paste:
                pasteFile();
                return true;
            case R.id.select_all:
                adapter.selectAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!adapter.leaveSelectMode()) {
            if (fab.isOpened()) {
                closeFloatingMenu();
            } else {
                super.onBackPressed();
            }
        }
    }


}
