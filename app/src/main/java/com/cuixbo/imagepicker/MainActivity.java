package com.cuixbo.imagepicker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.cuixbo.lib.imagepicker.ImagePickHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    public static final String[] CAMERA_AND_STORAGE = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final int PERMISSION_REQ_CODE = 434;

    private TextView mTextMessage;
    private ImageView mIvImage;
    private ImagePickHelper mImagePickHelper;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText("不裁剪");
                    mImagePickHelper
                            .showListDialog(false)
                            .setImagePickCallBack(new ImagePickHelper.ImagePickCallBack() {
                                @Override
                                public void onSuccess(Uri uri, final String path) {
                                    mIvImage.setImageURI(uri);
                                }
                            });
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText("裁剪");
                    mImagePickHelper
                            .showListDialog(true)
                            .setImagePickCallBack(new ImagePickHelper.ImagePickCallBack() {
                                @Override
                                public void onSuccess(Uri uri, final String path) {
                                    mIvImage.setImageURI(uri);
                                }
                            });

                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        mTextMessage = findViewById(R.id.message);
        mIvImage = findViewById(R.id.iv_image);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mImagePickHelper = new ImagePickHelper(this);
        checkPermission();
    }

    /**
     * 1.检查权限
     * 2.申请权限(异步)
     * 3.处理权限申请的回调
     * 可以封装一下,处理回调
     */
    private void checkPermission() {
        //检查权限
        boolean hasPermission = true;
        for (String perm : CAMERA_AND_STORAGE) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false;
                break;
            }
        }
        if (!hasPermission) {
            Log.e("xbc", "还没有授权");
            //申请权限(异步)
            ActivityCompat.requestPermissions(this, CAMERA_AND_STORAGE, PERMISSION_REQ_CODE);
        } else {
            Log.e("xbc", "已经授权了");
        }
    }

    /**
     * 处理权限申请的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean granted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                        break;
                    }
                }
            } else {
                granted = false;
            }
            if (granted) {
                Log.e("xbc", "被授权了");
            } else {
                Log.e("xbc", "授权被拒了");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mImagePickHelper.handleResult(requestCode, resultCode, data);
    }
}
