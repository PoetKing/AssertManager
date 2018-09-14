package com.poet.assetsmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * //
 * Created by WangYaDong on 2018/9/2.
 */
public class ExportActivity extends Activity {

    public static final String EXTRA_PATH = "srcPath";

    private ListView listView;
    private MyAdapter adapter;
    private String rootPath;
    private String srcPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        srcPath = getIntent().getStringExtra(EXTRA_PATH);
        listView = new ListView(this);
        setContentView(listView);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.goNext(new Grade(adapter.getItem(position).getPath()));
            }
        });
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        final Button newBtn = new Button(this);
        newBtn.setText("新建");
        Button importBtn = new Button(this);
        importBtn.setText("导入");
        layout.addView(newBtn);
        layout.addView(importBtn);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        ((FrameLayout) getWindow().getDecorView()).addView(layout, params);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == newBtn) {
                    newDir();
                } else {
                    importFile();
                }
            }
        };
        newBtn.setOnClickListener(listener);
        importBtn.setOnClickListener(listener);
    }

    private void newDir() {
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("新建", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = et.getText().toString().trim();
                        if (TextUtils.isEmpty(name)) {
                            return;
                        }
                        File dir = new File(adapter.deque.getLast().path, name);
                        if (dir.exists()) {
                            Toast.makeText(ExportActivity.this, "文件夹已存在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (dir.mkdir()) {
                            adapter.deque.getLast().refreshChildren();
                            adapter.goNext(new Grade(dir.getPath()));
                        }
                    }
                }).show();
    }

    private void importFile() {
        String dirPath = adapter.deque.getLast().path;
        String dstName = new File(srcPath).getName();
        int i = 0;
        for (; ; ) {
            String temp = i == 0 ? dstName : dstName + "-" + i;
            if (!new File(dirPath, temp).exists()) {
                dstName = temp;
                break;
            } else {
                i++;
            }
        }
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("loading")
                .setCancelable(false)
                .show();
        final String finalDstName = dstName;
        new Thread() {
            @Override
            public void run() {
                try (InputStream in = getAssets().open(srcPath);
                     FileOutputStream out =
                             new FileOutputStream(new File(adapter.deque.getLast().path, finalDstName))) {
                    final int total = in.available();
                    final AtomicInteger progress = new AtomicInteger(0);
                    byte[] buf = new byte[1024];
                    int read;
                    long last = 0;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        progress.addAndGet(read);
                        long curr = System.currentTimeMillis();
                        if (curr - last > 200) {
                            last = curr;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.setMessage(progress.get() + "/" + total);
                                }
                            });
                        }
                    }
                    out.flush();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ExportActivity.this, "导入成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        if (!adapter.goBack()) {
            super.onBackPressed();
        }
    }

    private class MyAdapter extends BaseAdapter {

        Deque<Grade> deque = new ArrayDeque<>();

        MyAdapter() {
            deque.add(new Grade());
            notifyDataSetChanged();
        }

        void goNext(Grade grade) {
            deque.getLast().position = listView.getFirstVisiblePosition();
            deque.add(grade);
            notifyDataSetChanged();
        }

        boolean goBack() {
            if (deque.size() >= 2) {
                deque.removeLast();
                notifyDataSetChanged();
                listView.setSelection(deque.getLast().position);
                return true;
            }
            return false;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            getActionBar().setTitle(deque.getLast().name);
        }

        @Override
        public int getCount() {
            if (deque.isEmpty()) {
                return 0;
            }
            return deque.getLast().children.size();
        }

        @Override
        public File getItem(int position) {
            return deque.getLast().children.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = new TextView(parent.getContext());
                int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources()
                        .getDisplayMetrics());
                tv.setPadding(p, p, p, p);
                convertView = tv;
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(getItem(position).getName());
            return convertView;
        }
    }

    private class Grade {
        String path;
        String name;
        List<File> children = new ArrayList<>();
        int position;

        Grade() {
            path = rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            name = "手机存储";
            refreshChildren();
        }

        Grade(String path) {
            this.path = path;
            this.name = path.replace(rootPath, "手机存储");
            refreshChildren();
        }

        void refreshChildren() {
            children.clear();
            File[] files = new File(path).listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        children.add(file);
                    }
                }
                Collections.sort(children);
            }
        }
    }
}
