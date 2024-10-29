package com.aspire.assess.activity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.AsyncTaskLoader;
import androidx.recyclerview.widget.RecyclerView;

import com.aspire.assess.R;
import com.aspire.assess.adapter.FileViewAdapter;
import com.aspire.assess.utils.FileManagerUtils;
import com.github.clans.fab.FloatingActionMenu;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;

public class SearchActivity extends BaseActivity {

    private AsyncTask searchTask = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(path);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        EditText searchInput = findViewById(R.id.search_input);
        searchInput.setVisibility(View.VISIBLE);

        searchInput.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                search();
            }
            return true;
        });

        FloatingActionMenu fab = findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
    }

    @Override
    protected void init() {
        super.init();

        RecyclerView recyclerView = findViewById(R.id.file_container);
        adapter = new FileViewAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    /**
     * 搜索文件
     */
    public void search() {
        //关闭搜索软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }

        EditText searchInput = findViewById(R.id.search_input);
        if (StringUtils.isBlank(searchInput.getText())) {
            return;
        }
        adapter.clearFiles();
        ProgressBar progressBar = findViewById(R.id.progress);
        searchTask = new SearchTask(this);

        try {
            progressBar.setVisibility(View.VISIBLE);
            searchTask.execute(path, searchInput.getText());
        } catch (Exception e) {
            Log.i("serarch", "search error", e);
            Toast.makeText(this, "搜索失败" + e.getMessage(), Toast.LENGTH_SHORT);
        }

    }

    //自定义异步任务
    static class SearchTask extends AsyncTask {
        private final ProgressBar progressBar;
        private final BaseActivity activity;

        public SearchTask(BaseActivity activity) {
            this.activity = activity;
            progressBar = activity.findViewById(R.id.progress);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            //搜索结果通过ui线程添加到视图上
            FileManagerUtils.instance.searchFiles(this, new File(objects[0].toString()), objects[1].toString(), file -> {
                activity.runOnUiThread(() -> activity.adapter.addFile(file));
            });
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Object o) {
            progressBar.setVisibility(View.GONE);
            super.onPostExecute(o);
        }

        @Override
        protected void onCancelled() {
            progressBar.setVisibility(View.GONE);
            super.onCancelled();
        }
    }
}
