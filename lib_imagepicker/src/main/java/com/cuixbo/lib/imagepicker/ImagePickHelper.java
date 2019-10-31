package com.cuixbo.lib.imagepicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Camera:  自己指定path,uri进行存储图片
 * Gallery: 使用系统返回的uri,查找出path
 * Crop:    自己指定path,uri进行存储图片
 * Created by xiaobocui on 2018/1/4.
 */
public class ImagePickHelper {

    public static ImagePickHelper imagePickHelper;

    public WeakReference<Activity> mWeakReference;

    public static String IMAGE_DIR;

    public static final int REQ_CODE_FROM_GALLERY = 313;// 相册
    public static final int REQ_CODE_FROM_CAMERA = 314;// 相机
    public static final int REQ_CODE_FROM_ZOOM = 315;// 裁剪

    private ImagePickCallBack mImagePickCallBack;

    public String mImagePathCamera;// 拍照的图片保存路径
    public String mImagePathGallery;// 相册的图片路径
    public String mImagePathResult;// 拍照、相册，最终处理完的图片保存路径
    public String mImagePathCrop;// 裁剪，最终Crop的图片保存路径

    private boolean mCrop;

    public ImagePickHelper(@NonNull Activity activity) {
        if (IMAGE_DIR == null) {
            File cacheDir = activity.getExternalCacheDir();
            if (cacheDir != null) {
                setCacheDir(cacheDir.getAbsolutePath() + "/image_cache");
            } else {
                setCacheDir(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ImagePicker/image_cache");
            }
        }
        clearImageCache();
        mWeakReference = new WeakReference<>(activity);
    }

    public static ImagePickHelper get(@NonNull Activity activity) {
        if (imagePickHelper != null && imagePickHelper.mWeakReference.get() == activity) {
            return imagePickHelper;
        }
        imagePickHelper = new ImagePickHelper(activity);
        return imagePickHelper;
    }

    /**
     * 设置缓存目录
     */
    public ImagePickHelper setCacheDir(String dir) {
        IMAGE_DIR = dir;
        return this;
    }

    public void setImagePickCallBack(ImagePickCallBack callBack) {
        mImagePickCallBack = callBack;
    }

    /**
     * 是否裁剪
     */
    public ImagePickHelper crop(boolean crop) {
        this.mCrop = crop;
        return this;
    }

    public ImagePickHelper showListDialog() {
        Activity activity = mWeakReference.get();
        if (activity == null) {
            return this;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("请选择");
        String[] itemStrings = new String[]{"拍照", "从手机相册选择", "取消"};
        builder.setItems(itemStrings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        takingByCamera();
                        break;
                    case 1:
                        takingByGallery();
                        break;
                    case 2:
                        dialog.dismiss();

                        break;
                }
            }
        });
        builder.create().show();
        return this;
    }

    public ImagePickHelper takingByCamera() {
        Activity activity = mWeakReference.get();
        if (activity == null) {
            return this;
        }
        if (!checkExternalStorage(activity)) {
            return this;
        }
        mImagePathCamera = generateImagePath("camera");//生成拍照图片存储的路径
        mImagePathResult = generateImagePath("result");//生成图片最终存储的路径
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, generateImageUri(activity, mImagePathCamera));// 这里的uri离开app，需要处理7.0兼容
        activity.startActivityForResult(intent, REQ_CODE_FROM_CAMERA);
        return this;
    }

    public ImagePickHelper takingByGallery() {
        Activity activity = mWeakReference.get();
        if (activity == null) {
            return this;
        }
        if (!checkExternalStorage(activity)) {
            return this;
        }
        mImagePathResult = generateImagePath("result");//生成图片最终存储的路径
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("return-data", false);
        activity.startActivityForResult(intent, REQ_CODE_FROM_GALLERY);
        return this;
    }

    /**
     * 裁剪图片
     *
     * @param path 原图的path
     */
    public ImagePickHelper takingByCrop(String path) {
        Activity activity = mWeakReference.get();
        if (activity == null) {
            return this;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(generateImageUri(activity, path), "image/*");//原图的uri离开app,需要兼容7.0
        // 7.0起，在调起相机拍照之后，调起裁切之前，需要加上下面两行：uri 权限，否则提示"图片加载失败"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 640);
        intent.putExtra("outputY", 640);
        intent.putExtra("return-data", false);
        mImagePathCrop = generateImagePath("crop");
        // 设置裁剪后的图片保存的uri.这里不能使用FileProvider，需要使用(file://)所以使用Uri.fromFile
        // 原因我猜测：setDataAndType中的uri,对于Crop软件来说是由外部传入的所以要用content uri，
        // 而下面output的uri（其实是Parcelable类型可能不会检测uri）是给CROP内部使用的所以用file uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mImagePathCrop)));// 裁剪后的uri，注意这里不需要处理7.0兼容
        activity.startActivityForResult(intent, REQ_CODE_FROM_ZOOM);
        return this;
    }

    /**
     * 检查外部存储是否已挂载
     */
    private boolean checkExternalStorage(Context context) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, "抱歉，外部存储尚未挂载", Toast.LENGTH_LONG).show();
            return false;
        }
        buildImageCacheDir();
        return true;
    }

    private void buildImageCacheDir() {
        File file = new File(IMAGE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 清除之前存储的图片缓存
     */
    private void clearImageCache() {
        try {
            File dir = new File(IMAGE_DIR);
            File[] files = dir.listFiles();
            if (files != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -3);// 3日内修改过的图片暂不删除
                for (File file : files) {
                    if (file.getName().startsWith("image_picker")) {
                        // 3日内修改过的图片暂不删除
                        if (calendar.after(file.lastModified())) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成唯一图片路径
     *
     * @return
     */
    private String generateImagePath(String type) {
        return IMAGE_DIR + "/image_picker_" + type + "_" + System.currentTimeMillis() + "_t.png";
    }

    /**
     * 生成图片uri传递给相机或裁剪，需要兼容7.0
     */
    private Uri generateImageUri(Context context, String imagePath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Uri.fromFile(new File(imagePath));//指定相册图片存储uri
        } else {
            // 7.0之后开始StrictMode 包含(file://)的uri离开应用会出现异常。所以需要用(content://)uri,FileProvider可以实现
            // content://com.dajie.official.chat.provider/external_files/DCIM/Camera/IMG_20190424_192129.jpg
            return FileProvider.getUriForFile(context, context.getPackageName() + ".imagepicker.provider", new File(imagePath));
        }
    }

    /**
     * 采样率获取bitmap，修正图片旋转角度，压缩输出到目标路径
     */
    public String fixImageDegree(Context context, String srcPath, String destPath) {
        int degree = BitmapUtils.getImageDegree(srcPath);
        // 从uri获取bitmap(采样率缩放)
        Bitmap bitmap = BitmapUtils.getInSampleBitmapFromUri(context, Uri.fromFile(new File(srcPath)));
        if (degree != 0) {// 需要修复图片旋转
            bitmap = BitmapUtils.rotateBitmap(bitmap, degree);//旋转图片
        }
        // compress
        BitmapUtils.compressBitmapToFile(bitmap, destPath, 100);
        return destPath;
    }

    public void handleResult(int requestCode, int resultCode, Intent data) {
        Activity activity = mWeakReference.get();
        if (activity == null) {
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQ_CODE_FROM_CAMERA://已经设置了output,图片会存到对应的uri(IMAGE_URI)
                // 需要处理旋转
                mImagePathResult = fixImageDegree(activity, mImagePathCamera, mImagePathResult);
                if (mCrop) {// 裁剪
                    takingByCrop(mImagePathResult);//进行裁剪
                } else if (mImagePickCallBack != null) {
                    mImagePickCallBack.onSuccess(Uri.fromFile(new File(mImagePathResult)), mImagePathResult);
                } else {
                    Toast.makeText(activity, "图片获取失败", Toast.LENGTH_LONG).show();
                    if (mImagePickCallBack != null) {
                        mImagePickCallBack.onFailed();
                    }
                }
                break;
            case REQ_CODE_FROM_GALLERY://data.getData()返回的是选中的图片原始uri,我们需要存到自己的uri中
                if (data != null && data.getData() != null) {//成功
                    //data content://com.miui.gallery.open/raw//storage/emulated/0/DCIM/Camera/IMG_20190424_192129.jpg
                    // 需要将data转为path
                    //path /storage/emulated/0/DCIM/Camera/IMG_20190424_192129.jpg
                    mImagePathGallery = BitmapUtils.getRealPathFromUri(activity, data.getData());
                    // 需要处理旋转（这里可能选的是拍照的那张被旋转的原图，所以也要处理旋转）
                    mImagePathResult = fixImageDegree(activity, mImagePathGallery, mImagePathResult);
                    if (mCrop) {
                        takingByCrop(mImagePathResult);//进行裁剪
                    } else if (mImagePickCallBack != null) {//根据返回uri取路径
                        mImagePickCallBack.onSuccess(Uri.fromFile(new File(mImagePathResult)), mImagePathResult);
                    }
                } else {
                    Toast.makeText(activity, "图片获取失败", Toast.LENGTH_LONG).show();
                    if (mImagePickCallBack != null) {
                        mImagePickCallBack.onFailed();
                    }
                }
                break;
            case REQ_CODE_FROM_ZOOM: //RESULT_OK即表示成功,因为我们指定了output
                if (mImagePickCallBack != null) {
                    mImagePickCallBack.onSuccess(Uri.fromFile(new File(mImagePathCrop)), mImagePathCrop);
                }
                break;
        }
    }

    public static abstract class ImagePickCallBack {
        public abstract void onSuccess(Uri uri, String path);

        public void onFailed() {
        }
    }

}
