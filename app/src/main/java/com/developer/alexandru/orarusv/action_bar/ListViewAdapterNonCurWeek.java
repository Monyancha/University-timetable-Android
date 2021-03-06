package com.developer.alexandru.orarusv.action_bar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.developer.alexandru.orarusv.Utils;
import com.developer.alexandru.orarusv.view_pager.CheckBoxOnChangeListener;
import com.developer.alexandru.orarusv.main.MainActivity;
import com.developer.alexandru.orarusv.R;
import com.developer.alexandru.orarusv.data.DBAdapter;
import com.developer.alexandru.orarusv.data.Course;
import com.developer.alexandru.orarusv.view_pager.DayListViewAdapter;

import java.util.ArrayList;


/**
 * Created by Alexandru on 7/2/14.
 * Adapter for the drop-down menu
 */
public class ListViewAdapterNonCurWeek extends BaseAdapter {

    private ArrayList<Course> values;
    private String[] daysName;
    private Context context;
    private MainActivity activity;
    private String backupFileName;
    private SharedPreferences weekProgress;

    public static final  int CURRENT = 1, NON_CURRENT = 0;
    private final int NUM_DAYS = 7;
    private final String NAME_FOR_TITLE_ELEMENT = "title";

    public ListViewAdapterNonCurWeek(MainActivity activity, String backupFileName, int week) {
        this.context = activity;
        this.activity = activity;

        daysName = activity.getResources().getStringArray(R.array.days_of_week_full_name);
        this.backupFileName = backupFileName;
        weekProgress = context.getSharedPreferences(this.backupFileName, Context.MODE_PRIVATE);

        new DataLoaderForNonCurrentWeek(activity).execute(week);
    }

    @Override
    public int getCount() {
        if(values == null)
            return 0;
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if(isTitleAt(position))
            return 1;
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DayListViewAdapter.ViewHolder viewHolder;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(isTitleAt(position)){
                convertView = inflater.inflate(R.layout.spinner_item_layout, parent, false);
                viewHolder = new DayListViewAdapter.ViewHolder();
                viewHolder.eventName = (TextView) convertView.findViewById(R.id.spinner_elem_tv);
                convertView.setTag(viewHolder);
            }else{
                convertView = inflater.inflate(R.layout.course_item_layout, parent, false);
                viewHolder = new DayListViewAdapter.ViewHolder();
                viewHolder.eventName = (TextView)convertView.findViewById(R.id.course_name);
                viewHolder.eventType = (TextView)convertView.findViewById(R.id.course_description);

                viewHolder.eventCheckBox = (CheckBox)convertView.findViewById(R.id.event_checkbox);
                convertView.setTag(viewHolder);
            }
        }else
            viewHolder = (DayListViewAdapter.ViewHolder) convertView.getTag();

        if(isTitleAt(position)){
            viewHolder.eventName.setText(values.get(position).getName());
            viewHolder.eventName.setGravity(Gravity.CENTER);
            convertView.setClickable(false);
        }
        else{
            final Course c = values.get(position);
            viewHolder.eventName.setText(c.getName());
            viewHolder.eventName.setText(c.getName().toUpperCase());
            viewHolder.eventType.setText(c.getType() + "\n" + c.getTime() + "\n" +
                    c.getLocation());

            viewHolder.eventCheckBox.setTag(backupFileName + ";" +
                    values.get(position).getName() + "_" +
                    values.get(position).getType());
            viewHolder.eventCheckBox.setOnCheckedChangeListener(new CheckBoxOnChangeListener());

            boolean currentCourseProgress;
            if(weekProgress == null)
                weekProgress = convertView.getContext().getSharedPreferences(backupFileName, Context.MODE_PRIVATE);

            currentCourseProgress = weekProgress.getBoolean(c.getName() + "_" +
                    c.getType(), false);
            if(currentCourseProgress)
                viewHolder.eventCheckBox.setChecked(true);
            else
                viewHolder.eventCheckBox.setChecked(false);

            RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.layout_course);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("CLICKED ON", c.getFullName());

                    activity.onCourseClicked(c);
                }
            });
         }
        return convertView;
    }

    private boolean isTitleAt(int position){
        if(values.get(position).getType().equals(NAME_FOR_TITLE_ELEMENT))
            return true;
        return false;
    }

    public ArrayList<Course> getValues(){
        return this.values;
    }

    private class DataLoaderForNonCurrentWeek extends AsyncTask<Integer, Void, ArrayList<Course>>{
        private Context context;
        private DBAdapter dbAdapter;
        public DataLoaderForNonCurrentWeek(Context context) {
            this.context = context;
        }

        @Override
        protected ArrayList<Course> doInBackground(Integer... integers) {
            final int week = integers[0];
            dbAdapter = new DBAdapter(context);
            dbAdapter.open();
            ArrayList<Course> courses = new ArrayList<Course>();
            String[] daysName = context.getResources().getStringArray(R.array.days_of_week_full_name);
            for(int i = 0; i < NUM_DAYS; i++){
                //Add name of day first
                courses.add(new Course(daysName[i].toUpperCase(),"", NAME_FOR_TITLE_ELEMENT, "", "" ,"", "", "", "", ""));
                courses.addAll(dbAdapter.getCourses(week, i, Utils.getCurrentTimetableId(this.context)));
            }

            return courses;
        }

        @Override
        protected void onPostExecute(ArrayList<Course> courses) {
            dbAdapter.close();
            values = new ArrayList<Course>();
            values.clear();
            values.addAll(courses);
            notifyDataSetChanged();

        }
    }

}
