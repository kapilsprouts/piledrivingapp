package com.bridge3;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class LogListActivity extends ListActivity {

    private LogData log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
//        setContentView(R.layout.activity_log_list);
        Intent intent = getIntent();
        log = intent.getParcelableExtra("logdata");
//        System.out.println("This is awesome");
//        for (int i=0; i < log.size(); i++) {
//            for (String s : log.getRow(i)) {
//                System.out.print(s+" | ");
//            }
//            System.out.println();
//        }

        String[][] values = new String[log.size()][4];

        for (int i=0; i < log.size(); i++) {
            for (int j=0; j < 4; j++) {
                values[i][j] = log.getRow(i).get(j);
            }
        }

        MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, values);

        setListAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_list, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public class MySimpleArrayAdapter extends ArrayAdapter<String[]> {
        private final Context context;
        private final String[][] values;

        public MySimpleArrayAdapter(Context context, String[][] values) {
            super(context, R.layout.rowlayout, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = null;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.rowlayout, parent, false);
            TextView textView1 = (TextView) rowView.findViewById(R.id.label1);
            TextView textView2 = (TextView) rowView.findViewById(R.id.label2);
            TextView textView3 = (TextView) rowView.findViewById(R.id.label3);
            TextView textView4 = (TextView) rowView.findViewById(R.id.label4);

            if (position == 0) {
                textView1.setTypeface(null, Typeface.BOLD);
                textView2.setTypeface(null, Typeface.BOLD);
                textView3.setTypeface(null, Typeface.BOLD);
                textView4.setTypeface(null, Typeface.BOLD);
            }
//            else {
//                textView2.setText(String.format("%.3f", new Double(values[position][1])));
//                textView3.setText(String.format("%.3f", new Double(values[position][2])));
//            }

            textView1.setText(values[position][0]);
            textView2.setText(values[position][1]);
            textView3.setText(values[position][2]);
            textView4.setText(values[position][3]);
            return rowView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("Option selected");
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
