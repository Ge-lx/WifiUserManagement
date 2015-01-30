package io.github.gelx_.wifiusermanagement;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

import gelx_.github.io.wifiusermanagement.R;
import io.github.gelx_.wifiusermanagement.database.DB_users;


public class DB_userActivity extends Activity {

    private DB_users user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_user);

        Intent intent = getIntent();
        String username = intent.getStringExtra(MyActivity.EXTRA_USERNAME);
        new LoadUserAsyncTask().execute(username);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.db_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonPress(View button){
        AlertDialog dialog = new AlertDialog.Builder(DB_userActivity.this).create();
        dialog.setTitle("Delete user?");
        dialog.setMessage("This user will be deleted!");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Confirm", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE)
                    new Thread(new Runnable(){
                        public void run(){
                            MyActivity.instance.getConnectionHelper().deleteUser(user.getName());
                            finish();
                        }
                    }).start();
                Toast toast = Toast.makeText(getApplicationContext(), "User deleted!", Toast.LENGTH_SHORT);
                toast.show();
                MyActivity.instance.refreshUserList();
            }
        });
        dialog.show();
    }

    public class LoadUserAsyncTask extends AsyncTask<String, Void, DB_users> {

        @Override
        protected DB_users doInBackground(String... params) {
            try {
                ConnectionHelper connectionHelper = MyActivity.instance.getConnectionHelper();
                return connectionHelper.getUser(params[0]);
            } catch (TimeoutException e) {
                Log.e("DB_userActivity", "Server did not respond in time!");
                return null;
            }
        }

        @Override
        protected void onPostExecute(DB_users db_users) {
            user = db_users;
            EditText name = (EditText) findViewById(R.id.editText);
            EditText mac = (EditText) findViewById(R.id.editText2);
            EditText code = (EditText) findViewById(R.id.editText4);
            EditText expiresIn = (EditText) findViewById(R.id.editText3);
            CheckBox expired = (CheckBox) findViewById(R.id.checkBox);
            name.setText(db_users.getName());
            mac.setText(db_users.getMac());
            code.setText(db_users.getCode());
            long expiresIn1 = db_users.getExpires() - System.currentTimeMillis();
            if(expiresIn1 < 0) {
                expiresIn.setText("Expired");
                expired.setChecked(true);
            } else {
                int hours = (int) (expiresIn1 / 3600000);
                int minutes = (int) ((expiresIn1 - (hours * 3600000)) / 60000);
                int seconds = (int) ((expiresIn1 - (hours * 3600000) - (minutes * 60000)) / 1000);
                expiresIn.setText(hours + "h " + minutes + "m " + seconds + "s");
                expired.setChecked(false);
            }
        }
    }
}
