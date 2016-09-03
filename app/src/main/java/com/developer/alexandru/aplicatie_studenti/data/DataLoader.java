package com.developer.alexandru.aplicatie_studenti.data;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.developer.alexandru.aplicatie_studenti.MainActivity;
import com.developer.alexandru.aplicatie_studenti.R;
import com.developer.alexandru.aplicatie_studenti.TimetableFragment;
import com.developer.alexandru.aplicatie_studenti.Utils;
import com.developer.alexandru.aplicatie_studenti.app_widget.ListRemoteViewsFactory;
import com.developer.alexandru.aplicatie_studenti.view_pager.PagerSlidingTabStrip;
import com.developer.alexandru.aplicatie_studenti.view_pager.ViewPagerAdapter;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Alexandru on 6/14/14.
 * Loads the data from the local database both for the view pager and for the app widgets.
 * The calls to update the app widgets will be distinguished by the calls to update the activity_main activity
 * with the help of the parameters from the constructor
 */
public class DataLoader extends AsyncTask<Void, Void, Void> {
    //Debug
    public static final String TAG = "DataLoader";
    public static final boolean D = true;

    private Context context;
    private MainActivity activity;

    private ViewPager viewPager;
    private int currentItem;
    private PagerSlidingTabStrip pagerSlidingTabStrip;
    private FragmentManager fragmentManager;

    private ListRemoteViewsFactory listRemoteViewsFactory;
    private String widgetProviderName;
    public static boolean widgetForTomorrow;
    private AppWidgetManager appWidgetManager;

    private TimetableFragment.OnCourseSelected onCourseSelected ;

    /**
     *  Constructor for the async task loading the data from the local database
     * @param onCourseSelected interface implemented by the activity (determines the click event)
     * @param childFragManager fragment manager of the fragment which contains the view pager which will handle the data
     * @param current the current position of the view pager
     * @param listRemoteViewsFactory if the call is from the widget service this is NOT null
     * @param context if the call is from the widget service this is NOT null (context of the service)
     * @param widgetProviderName if the call is from the widget service this is not null
     */
    public DataLoader(TimetableFragment.OnCourseSelected onCourseSelected , FragmentManager childFragManager, int current,
                      ListRemoteViewsFactory listRemoteViewsFactory, Context context, String widgetProviderName){
        if(onCourseSelected != null){
            //Thread spawn by activity_main activity
            this.onCourseSelected = onCourseSelected;
            this.activity = onCourseSelected.getActivity();
            this.viewPager = (ViewPager) activity.findViewById(R.id.view_pager);
            //Current item was sent "encoded" incremented by 1
            this.currentItem = current - 1;
            this.pagerSlidingTabStrip = (PagerSlidingTabStrip) activity.findViewById(R.id.sliding_tabs);
            this.fragmentManager = childFragManager;
        }
        else{
            //Thread spawn by remote views service for widget
            this.listRemoteViewsFactory = listRemoteViewsFactory;
//            this.context = activity;
//            ViewPagerAdapter.context = context;
            this.context = context;
            this.widgetProviderName = widgetProviderName;

        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        DBAdapter dbAdapter = null;
        SharedPreferences timeOrganiser;
        if (activity != null)
            timeOrganiser = activity.getSharedPreferences(MainActivity.TIME_ORGANISER_FILE_NAME,
                Context.MODE_PRIVATE);
        else
            timeOrganiser = context.getSharedPreferences(MainActivity.TIME_ORGANISER_FILE_NAME,
                    Context.MODE_PRIVATE);
        //Get the current week ( this will be updated either by opening the app or without the user intervention by the widget
        //service
        final int currentWeek = timeOrganiser.getInt(MainActivity.WEEK_OF_SEMESTER, MainActivity.WEEKS_IN_SEMESTER);
        if(activity != null){
            try {
                 //ViewPagerAdapter.context = activity;

                //Open the connection with the local database

                //Thread created by the fragment hosting the viewpager
                dbAdapter = new DBAdapter(activity);
                dbAdapter.open();

//                if (ViewPagerAdapter.listsOfCourses == null)
//                    ViewPagerAdapter.listsOfCourses = new ArrayList[ViewPagerAdapter.NUM_DAYS];
//
//                for (int i = 0; i < ViewPagerAdapter.NUM_DAYS; i++) {
//                    ViewPagerAdapter.listsOfCourses[i] = dbAdapter.getCourses(currentWeek, i);
//                }

            } catch (ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }finally{
                if (dbAdapter != null)
                    dbAdapter.close();
            }
        }

        if(context != null && listRemoteViewsFactory != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                try {
                    //Thread created by widget service
                    dbAdapter = new DBAdapter(context);
                    dbAdapter.open();

                    if (D) Log.d("LOADING THREAD", "from widget service");
                    appWidgetManager = AppWidgetManager.getInstance(context);

                    Calendar calendar = Calendar.getInstance();
                    final int dayOfWeek = calendar.get(Calendar.MINUTE) % 7 + 1; //calendar.get(Calendar.DAY_OF_WEEK);
                    if (widgetForTomorrow) {
                        //The classes for tomorrow are needed
                        ArrayList<Course> tomorrowCourses;
                        tomorrowCourses = dbAdapter.getCourses(currentWeek, dayOfWeek);
                        listRemoteViewsFactory.setValues(tomorrowCourses);
                    } else {

                        final ArrayList<Course> todayCourses = dbAdapter.getCourses(currentWeek, dayOfWeek);
                        listRemoteViewsFactory.setValues(todayCourses);
                        if (D) Log.d(TAG, "" + dayOfWeek);
                    }
                }catch (ArrayIndexOutOfBoundsException e){
                    e.printStackTrace();
                } finally {
                    if (dbAdapter != null)
                        dbAdapter.close();
                }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        // The list view adapter for every day of current week uses the current week value.
        //Also the remote views adapter uses it.Must be set now.

        if(activity != null){
            Utils.setCurrentWeek(activity);
            //Thread was created by activity_main activity
            if(D) Log.d(TAG, "from activity_main");
            ViewPagerAdapter adapter = null;
            if (viewPager != null) {
                if (viewPager.getAdapter() == null) {
                    adapter = new ViewPagerAdapter(onCourseSelected, fragmentManager);
                    viewPager.setAdapter(adapter);
                    pagerSlidingTabStrip.setViewPager(viewPager);
                } else {
                    //The view pager exists.Change only it's data
                    if (D) Log.d(TAG, "view pager old adapter");
                    viewPager.getAdapter().notifyDataSetChanged();
                    //viewPager.invalidate();
                }

                //Update last displayed frag.This will be void because it's onResume was already called
                if (currentItem != -1)
                    if (D) Log.d(TAG, "ALL IS UPDATED");

                //final int lastVPPosition = activity.retrieveLastPosition();
                Calendar cal = Calendar.getInstance();
                viewPager.setCurrentItem(cal.get(Calendar.DAY_OF_WEEK) - 1);
                if (D) Log.d("POST LOADING", "" + cal.get(Calendar.DAY_OF_WEEK));
            }
        }else {
            //Thread was created by the widget service
            Utils.setCurrentWeek(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                //Notify all the widgets that the underlying data has changed
                final ComponentName appWidget = new ComponentName(context, widgetProviderName);
                if (appWidgetManager != null) {
                    final int[] appWidgetsId = appWidgetManager.getAppWidgetIds(appWidget);
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetsId, R.id.widget_list);
                    Log.d(TAG, widgetProviderName);
                }
            }
        }
    }

    /**
     * Check if a course is in a week
     * @param week the week of the semester
     * @param c the course to be verified
     * @return
     */
    public static boolean isCourseInWeek(int week, Course c){
        if (!c.info.equals(ViewPagerAdapter.COURSES_IN_EVEN_WEEK) && !c.info.equals(ViewPagerAdapter.COURSES_IN_ODD_WEEK))
            return true;

        if ((c.info.equals(ViewPagerAdapter.COURSES_IN_ODD_WEEK) && (week % 2 == 1)) ||
                (c.info.equals(ViewPagerAdapter.COURSES_IN_EVEN_WEEK) && week % 2 == 0))
                    return true;

        return false;
    }

}
