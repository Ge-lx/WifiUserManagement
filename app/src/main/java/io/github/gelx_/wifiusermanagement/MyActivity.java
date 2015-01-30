package io.github.gelx_.wifiusermanagement;

import android.app.Activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;

import gelx_.github.io.wifiusermanagement.R;
import io.github.gelx_.wifiusermanagement.database.DB_users;


public class MyActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
                   ItemFragment.OnFragmentInteractionListener,
                   AddUserFragment.OnFragmentInteractionListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    public static final String EXTRA_USERNAME = "io.github.gelx_.wifiusermanagement.USERNAME";

    public static MyActivity instance;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private boolean showingUserList;
    private ItemFragment userListFragment;

    private ConnectionHelper connectionHelper;
    private Thread connHelperThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //TODO: Read from config or something
        final InetSocketAddress address = new InetSocketAddress("192.168.2.109", 12345);
        connHelperThread = new Thread(new Runnable(){ public void run(){
            try {
                connectionHelper = new ConnectionHelper(address, getApplicationContext());
            } catch (IOException e) {
                Log.e("Activity", "Error creating ConnectionHelper: " + e.getMessage());
            }
        }});
        connHelperThread.start();

        instance = this; // :'(
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        showingUserList = false;

        switch(position) {
            case 0:
                this.userListFragment = new ItemFragment();
                fragment = this.userListFragment;
                showingUserList = true;
                break;
            case 1:
                fragment = new AddUserFragment();
                break;
            default:
                fragment = PlaceholderFragment.newInstance(position + 1);
                break;
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            if(showingUserList)
                getMenuInflater().inflate(R.menu.userlist, menu);
            else
                getMenuInflater().inflate(R.menu.my, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            refreshUserList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(DB_users user) {
        Log.d("ACTIVITY", user.toString());
        Intent intent = new Intent(this, DB_userActivity.class);
        intent.putExtra(EXTRA_USERNAME, user.getName());
        startActivity(intent);
    }

    public ConnectionHelper getConnectionHelper(){
        if(connectionHelper == null && connHelperThread.isAlive())
            try {
                connHelperThread.join(5000);
            } catch (InterruptedException e) {
                Log.e("Activity", "Could not get ConnHelper in 5s");
            }

        return connectionHelper;
    }

    public void refreshUserList(){
        if(!showingUserList){
            throw new IllegalStateException("Not showing UserList!");
        } else {
            this.userListFragment.refresh();
        }
    }

    public void onUserAdd(View view){
        TextView nameTextView = (TextView) findViewById(R.id.editText);
        TextView expiresTextView = (TextView) findViewById(R.id.editText3);
        String name = nameTextView.getText().toString();
        int expires = Integer.parseInt(expiresTextView.getText().toString());

        final DB_users user = new DB_users(name, expires);
        String code = user.getCode();

        new Thread(new Runnable(){ public void run(){
            getConnectionHelper().addUser(user);
        }
        }).start();
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(expiresTextView.getWindowToken(), 0);

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setTitle("User created!");
        dialog.setMessage("Code: '" + code + "'");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onNavigationDrawerItemSelected(0);
            }
        });
        dialog.show();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_my, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MyActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
