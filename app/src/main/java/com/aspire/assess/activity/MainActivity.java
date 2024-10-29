package com.aspire.assess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.aspire.assess.R;
import com.aspire.assess.utils.GetFilesUtils;

import java.io.File;

public class MainActivity extends BaseActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void init() {
        super.init();
        if (!path.equals(GetFilesUtils.getInstance().getBasePath())) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        setTitle(new File(path).getName());
        getSupportActionBar().setSubtitle(path);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.search) {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        init();
        adapter.notifyDataSetChanged();
        super.onResume();
    }
}
