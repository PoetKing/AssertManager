package com.poet.assetsmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    private MyAdapter adapter = new MyAdapter();
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelected(position);
            }
        });
        listView.setAdapter(adapter);
        setContentView(listView);
        try {
            String[] list = getAssets().list("root");
            adapter.setData(list);
        } catch (Exception e) {
            e.printStackTrace();
        }

        button = new Button(this);
        button.setText("export");
        button.setEnabled(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExportActivity.class);
                intent.putExtra(ExportActivity.EXTRA_PATH, "root" + File.separator + adapter.getSelected());
                startActivity(intent);
                adapter.clearSelected();
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        ((FrameLayout) getWindow().getDecorView()).addView(button, params);
    }

    private class MyAdapter extends BaseAdapter {

        private String[] data = new String[0];
        private int selected = -1;

        void setData(String[] list) {
            data = list;
            notifyDataSetChanged();
        }

        void setSelected(int selected) {
            if (this.selected == selected) {
                this.selected = -1;
            } else {
                this.selected = selected;
            }
            notifyDataSetChanged();
        }

        void clearSelected() {
            selected = -1;
            notifyDataSetChanged();
        }

        String getSelected() {
            if (selected != -1) {
                return data[selected];
            }
            return null;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            button.setEnabled(selected != -1);
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public String getItem(int position) {
            return data[position];
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
            tv.setText(getItem(position));
            tv.setBackgroundColor(position == selected ? Color.RED : Color.TRANSPARENT);
            return convertView;
        }
    }
}
