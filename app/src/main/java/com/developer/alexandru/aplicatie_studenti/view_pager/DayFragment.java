package com.developer.alexandru.aplicatie_studenti.view_pager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.developer.alexandru.aplicatie_studenti.MainActivity;
import com.developer.alexandru.aplicatie_studenti.R;
import com.developer.alexandru.aplicatie_studenti.TimetableFragment;
import com.developer.alexandru.aplicatie_studenti.data.Course;
import com.developer.alexandru.aplicatie_studenti.data.DayCoursesLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Alexandru on 6/13/14.
 * Fragment belonging to the view pager. Contains the courses of a specific day.
 * Inflates a normal list layout.
 */
public class DayFragment extends ListFragment {
    //Debug
    public static final String TAG = "DayFragment";
    public static final boolean D = true;

    private String title;
    private int day;
    private int week;

    private ProgressDialog dialog;

    //public static TimetableFragment.OnCourseSelected onCourseSelected;
    private WeakReference<DayCoursesLoader> taskLoaderReference;
    private ArrayList<Course> list;
    private MyListViewAdapter adapter;
    DayCoursesLoader dayCoursesLoader;


    public static DayFragment createFragment(String title, int week, int day){
        //DayFragment.onCourseSelected = onCourseSelected;

        DayFragment dayFragment = new DayFragment();
        Bundle args = new Bundle();
        args.setClassLoader(Course.class.getClassLoader());
        args.putString("title", title);
        args.putInt("day", day);
        args.putInt("week", week);
        dayFragment.setArguments(args);

        return dayFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setHasOptionsMenu(true);
        if (savedInstanceState != null){
            if (D) Log.d(TAG, " create from previous state");
            day = savedInstanceState.getInt("day");
            list = savedInstanceState.getParcelableArrayList("courses");
        }
        if(args != null){
            if (D) Log.d(TAG, " created with args");
            title = args.getString("title");
            day = args.getInt("day");
            week = args.getInt("week");
            if (list == null)
                list = new ArrayList<>();
            adapter = new MyListViewAdapter((MainActivity)getActivity(), list);
            setListAdapter(adapter);
            taskLoaderReference = new WeakReference<>(new DayCoursesLoader(getActivity(), this));   // getActivity() will be not null
            //taskLoaderReference.get().execute(day, 8);                                       // see TimetableFragment
            if(D) Log.d(TAG, "Fragment " + title + " created");
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainActivity.REQUEST_CODE_DOWNLOAD)
            if (resultCode == Activity.RESULT_OK){
                if (D) Log.d(TAG, "data downloaded");
                this.list = null;
                adapter.notifyDataSetChanged();
                dayCoursesLoader = new DayCoursesLoader(getActivity(), this);
                taskLoaderReference = new WeakReference<>(dayCoursesLoader);
                dayCoursesLoader.execute(day, week);
            } else
                if (D) Log.d(TAG, "Not downloaded");
    }

    // Check for previously saved state
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View frag = inflater.inflate(R.layout.fragment_day, container, false);

        //checked for saved instance (mainly in case of screen rotation)
        /*if(savedInstanceState != null){
            if(D) Log.d(TAG, "RESTORE");

            int day = savedInstanceState.getInt("day");
            if(ViewPagerAdapter.listsOfCourses == null)
                ViewPagerAdapter.listsOfCourses = new ArrayList[ViewPagerAdapter.NUM_DAYS];
            try{
                if(ViewPagerAdapter.listsOfCourses[day] == null){
                    ViewPagerAdapter.listsOfCourses[day] = savedInstanceState.getParcelableArrayList("courses");
                    if(ViewPagerAdapter.listsOfCourses[day] == null)
                        if(D) Log.d("DAY FRAG", "nothing changed");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if(D) Log.d("FRAGMENT", "adapter from saved state" + title);
            if(this.getListAdapter() == null){
                this.setListAdapter(new MyListViewAdapter((MainActivity)getActivity(), ViewPagerAdapter.listsOfCourses[day]));

                if(D) Log.d("FRAGMENT", "adapter set " + title);
                //this.getListView().setItemsCanFocus(false);
            }else {
                if(D) Log.d("DAY FRAGMENT RESUMED", "old adapter");
            }
        }*/

        if (list == null || list.size() == 0) {
            if (savedInstanceState != null) {
                day = savedInstanceState.getInt("day");
                if (D) Log.d(TAG, "onCreateView " + "day " + day + " from previous");
                list = savedInstanceState.getParcelableArrayList("courses");
                if (adapter == null)
                    adapter = ((MyListViewAdapter)getListAdapter());
                if (adapter == null) {
                    adapter = new MyListViewAdapter((MainActivity) getActivity(), list);
                    setListAdapter(adapter);
                } else {
                    if (D) Log.d(TAG, "size " + list.size() );

                    adapter.setValues(list);
                    adapter.notifyDataSetChanged();
                }
            } else if (taskLoaderReference != null) {
                if (D) Log.d(TAG, "onCreateView " + " available reference ");
                DayCoursesLoader dayCoursesLoader = taskLoaderReference.get();
                if (dayCoursesLoader != null && !dayCoursesLoader.isCancelled())
                    dayCoursesLoader.cancel(true);
                dayCoursesLoader = new DayCoursesLoader(getActivity(), this);
                taskLoaderReference = new WeakReference<>(dayCoursesLoader);
                dayCoursesLoader.execute(day, week);
            } else {

                taskLoaderReference = new WeakReference<>(new DayCoursesLoader(getActivity(), this));
                taskLoaderReference.get().execute(day, week);
            }
        }
        return frag;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (taskLoaderReference != null){
            DayCoursesLoader dayCoursesLoader = taskLoaderReference.get();
            if (dayCoursesLoader != null)
                dayCoursesLoader.cancel(true);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        /*try {
            if(ViewPagerAdapter.listsOfCourses == null)
                ViewPagerAdapter.listsOfCourses = new ArrayList[ViewPagerAdapter.NUM_DAYS];
            this.setListAdapter(new MyListViewAdapter((MainActivity)getActivity(), ViewPagerAdapter.listsOfCourses[day]));
        }catch (NullPointerException e){
            e.printStackTrace();
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        if(D) Log.d(TAG, title + "  resumed");
    }

    /**
     * Saves all the courses in a bundle to be restored at recreation.
     * Used in some special cases: activity reconfiguration (screen rotation) or only a few fragments of the view pager are removed from memory.
    */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.setClassLoader(Course.class.getClassLoader());
        outState.putInt("day", day);
        outState.putInt("week", week);
        /*if (!ViewPagerAdapter.isAnyListNull()) {
            if (ViewPagerAdapter.listsOfCourses[day] != null)
                outState.putParcelableArrayList("courses", ViewPagerAdapter.listsOfCourses[day]);
            else
                outState.putParcelableArrayList("courses", ((MyListViewAdapter) getListAdapter()).getValues());
        }*/
        outState.putParcelableArrayList ("courses", ((MyListViewAdapter) getListAdapter()).getValues());
        super.onSaveInstanceState(outState);
        if (D) Log.d(TAG, "SAVE");
    }
    
}
