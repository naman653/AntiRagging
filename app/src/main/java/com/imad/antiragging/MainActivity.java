package com.imad.antiragging;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_REQUEST_CODE = 132;
    private FirebaseAuth auth;
    private ServiceResultReceiver serviceResultReceiver;
    private NavigationView navigationView;
    private AppBarConfiguration mAppBarConfiguration;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_sos, R.id.nav_locate,
                R.id.nav_squad, R.id.nav_about)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        setUpUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.action_logout);
            dialog.setMessage("Are you sure you want to Sign Out");
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("Notification");
                    auth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();
            return true;
        } else if(id == R.id.action_download) {
            if(ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_REQUEST_CODE);
            } else {
                serviceResultReceiver = new ServiceResultReceiver(new Handler());
                String url = getString(R.string.guideline_url);
                Intent intent = new Intent(MainActivity.this, DownloadService.class);
                intent.putExtra("receiver", serviceResultReceiver);
                intent.putExtra("url", url);
                startService(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void setUpUi(){
        View header = navigationView.getHeaderView(0);
        ImageView profile = header.findViewById(R.id.profile_image);
        TextView name = header.findViewById(R.id.name);
        TextView email = header.findViewById(R.id.email);


        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null) {
            if(currentUser.getPhotoUrl() == null || currentUser.getPhotoUrl().toString().isEmpty())
                Glide.with(this).load(R.drawable.ic_person_black_24dp).into(profile);
            else
                Glide.with(this).load(currentUser.getPhotoUrl()).into(profile);
            name.setText(currentUser.getDisplayName());
            email.setText(currentUser.getEmail());
        } else {
            Glide.with(this).load(R.drawable.ic_person_black_24dp).into(profile);
            name.setText("");
            email.setText("");
        }
    }

    class ServiceResultReceiver extends ResultReceiver {
        public ServiceResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode){
                case DownloadService.DOWNLOAD_SUCCESS:
                    String file_path = resultData.getString("file_path");
                    Toast.makeText(getApplicationContext(), R.string.download_successful, Toast.LENGTH_SHORT).show();
                    break;
            }
            super.onReceiveResult(resultCode, resultData);
        }
    }
}