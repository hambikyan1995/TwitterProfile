package com.task.twitterprofiletask;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ImageView avatar;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private View viewToolbarBg;
    private TextView tvName;
    private Bitmap image = null;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;
    private String displayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = FirebaseStorage.getInstance("gs://twittertask-d2634.appspot.com");
        displayName = getDisplayName();

        avatar = findViewById(R.id.avatar);
        tabLayout = findViewById(R.id.tabs);
        toolbar = findViewById(R.id.toolbar);
        tvName = findViewById(R.id.tvName);
        viewToolbarBg = findViewById(R.id.viewToolbarBg);

        initAppBar();
        initTabBar();
        setDetails();

        avatar.setOnClickListener(view -> selectImage());
    }

    private void setDetails() {
        loadProfileImage();

        if (displayName != null)
            tvName.setText(displayName);
        else tvName.setVisibility(View.GONE);
    }

    private void initTabBar() {
        tabLayout.addTab(tabLayout.newTab().setText("Tweets"));
        tabLayout.addTab(tabLayout.newTab().setText("Tweets and replies"));
        tabLayout.addTab(tabLayout.newTab().setText("Media"));
        tabLayout.addTab(tabLayout.newTab().setText("Likes"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    }

    private void initAppBar() {
        toolbar.setNavigationIcon(R.drawable.back);
        toolbar.setNavigationOnClickListener(view -> finish());

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        avatar.post(() -> appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                    avatar.setPivotY(avatar.getHeight());
                }

                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(displayName);
                    toolbar.setBackgroundColor(Color.BLACK);
                    isShow = true;
                } else {
                    if (isShow) {
                        collapsingToolbarLayout.setTitle(" ");
                        isShow = false;
                        toolbar.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.purple_500));
                    }
                }

                int diff = collapsingToolbarLayout.getHeight() - viewToolbarBg.getHeight();
                if (Math.abs(verticalOffset) >= scrollRange - diff)
                    return;

                float scale = 1 + (verticalOffset * 0.004f);
                avatar.setScaleY(scale);
                avatar.setScaleX(scale);
            }
        }));
    }

    private String getDisplayName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (!isNullOrBlank(user.getDisplayName()))
                return user.getDisplayName();
            if (!isNullOrBlank(user.getEmail()))
                return user.getEmail();
            if (!isNullOrBlank(user.getPhoneNumber()))
                return user.getPhoneNumber();
        }
        return "";
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 2) {
                uploadImage(data.getData());
            }
        }
    }

    private void uploadImage(Uri uri) {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            String avatarName = getAvatarName();
            if (avatarName == null) return;
            UploadTask uploadTask = storage.getReference().child("images/" + avatarName).putBytes(byteArray);
            uploadTask.addOnFailureListener(exception -> {
                dismissProgressDialog();
                Toast.makeText(MainActivity.this, "Could not upload image", Toast.LENGTH_SHORT).show();
            }).addOnSuccessListener(taskSnapshot -> {
                avatar.setImageBitmap(bitmap);
                dismissProgressDialog();
                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private String getAvatarName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) return user.getUid();
        return null;
    }

    private void loadProfileImage() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        storage.getReference().child("images/" + getAvatarName()).getDownloadUrl().addOnSuccessListener(this, uri -> {
            if (uri == null || uri.getPath().isEmpty())
                return;

            executor.execute(() -> {
                try {
                    InputStream in = new URL(uri.toString()).openStream();
                    image = BitmapFactory.decodeStream(in);
                    runOnUiThread(() -> avatar.setImageBitmap(image));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    public boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}