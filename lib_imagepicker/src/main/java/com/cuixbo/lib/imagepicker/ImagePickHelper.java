package com.cuixbo.lib.imagepicker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

/**
 * Camera:  自己指定path,uri进行存储图片
 * Gallery: 使用系统返回的uri,查找出path
 * Crop:    自己指定path,uri进行存储图片
 * Created by xiaobocui on 2018/1/4.
 */
public class ImagePickHelper {


    public static String IMAGE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Dajie/image_cache";

    public static final int REQ_CODE_FROM_GALLERY = 313;// 相册
    public static final int REQ_CODE_FROM_CAMERA = 314;// 相机
    public static final int REQ_CODE_FROM_ZOOM = 315;// 裁剪

    private Activity mActivity;
    private ImagePickCallBack mImagePickCallBack;

    public String mImagePathCamera;// 拍照的图片保存路径
    public String mImagePathGallery;// 相册的图片路径
    public String mImagePathResult;// 拍照、相册，最终处理完的图片保存路径
    public String mImagePathCrop;// 裁剪，最终Crop的图片保存路径

    private boolean mCrop;

    public ImagePickHelper(@NonNull Activity activity) {
        mActivity = activity;
    }

    public void setImagePickCallBack(ImagePickCallBack callBack) {
        mImagePickCallBack = callBack;
    }

    public ImagePickHelper showListDialog(final boolean crop) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("请选择");
        String[] itemStrings = new String[]{"拍照", "从手机相册选择", "取消"};
        builder.setItems(itemStrings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        takingByCamera(crop);
                        break;
                    case 1:
                        takingByGallery(crop);
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

    private void buildSDCardDirs() {
        File file = new File(IMAGE_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    //相机,不裁剪
    public void takingByCamera() {
        takingByCamera(false);
    }

    public void takingByCamera(boolean crop) {
        if (!checkSDCard()) {
            return;
        }
        buildSDCardDirs();
        clearImageCache();
        mImagePathCamera = generateImagePath("camera");//生成拍照图片存储的路径
        mImagePathResult = generateImagePath("result");//生成图片最终存储的路径
        mCrop = crop;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, generateImageUri(mImagePathCamera));// 这里的uri离开app，需要处理7.0兼容
        mActivity.startActivityForResult(intent, REQ_CODE_FROM_CAMERA);
    }

    //相册,不裁剪
    public void takingByGallery() {
        takingByGallery(false);
    }

    public void takingByGallery(boolean crop) {
        if (!checkSDCard()) {
            return;
        }
        buildSDCardDirs();
        clearImageCache();
        mImagePathResult = generateImagePath("result");//生成图片最终存储的路径
        mCrop = crop;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("return-data", false);
        mActivity.startActivityForResult(intent, REQ_CODE_FROM_GALLERY);
    }

    /**
     * 裁剪图片
     *
     * @param path 原图的path
     */
    public void takingByCrop(String path) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(generateImageUri(path), "image/*");//原图的uri离开app,需要兼容7.0
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
        mActivity.startActivityForResult(intent, REQ_CODE_FROM_ZOOM);
    }

    /**
     * 检查SD卡
     *
     * @return
     */
    private boolean checkSDCard() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(mActivity, "没有外置sdcard", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * 清除之前存储的图片缓存
     */
    private void clearImageCache() {
        try {
            File dir = new File(IMAGE_DIR);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("image_picker")) {
                        file.delete();
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
     *
     * @return
     */
    private Uri generateImageUri(String imagePath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Uri.fromFile(new File(imagePath));//指定相册图片存储uri
        } else {
            // 7.0之后开始StrictMode 包含(file://)的uri离开应用会出现异常。所以需要用(content://)uri,FileProvider可以实现
            // content://com.dajie.official.chat.provider/external_files/DCIM/Camera/IMG_20190424_192129.jpg
            return FileProvider.getUriForFile(mActivity, mActivity.getPackageName() + ".provider", new File(imagePath));
        }
    }

    /**
     * 将选择的原始图片存储到本地指定路径
     * 这里进行了采样率、体积压缩
     *
     * @param srcPath  选择的原始图片路径
     * @param destPath 本地指定路径
     */
    private void saveImage(String srcPath, String destPath) {
        Log.e("xbc", "saveImage:src:" + srcPath + ",dest:" + destPath);
        BitmapUtils.saveInSampleToFile(mActivity, srcPath, destPath);
    }

    /**
     * 采样率获取bitmap，修正图片旋转角度，压缩输出到目标路径
     *
     * @param
     * @param
     * @return
     */
    public String fixImageDegree(String srcPath, String destPath) {
        int degree = BitmapUtils.getImageDegree(srcPath);
        // 从uri获取bitmap(采样率缩放)
        Bitmap bitmap = BitmapUtils.getInSampleBitmapFromUri(mActivity, Uri.fromFile(new File(srcPath)));
        if (degree != 0) {// 需要修复图片旋转
            bitmap = BitmapUtils.rotateBitmap(bitmap, degree);//旋转图片
        }
        // compress
        BitmapUtils.compressBitmapToFile(bitmap, destPath, 100);
        return destPath;
    }

    public void handleResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQ_CODE_FROM_CAMERA://已经设置了output,图片会存到对应的uri(IMAGE_URI)
                Log.e("xbc", "degree:" + BitmapUtils.getImageDegree(mImagePathCamera));
                //需要处理旋转
                mImagePathResult = fixImageDegree(mImagePathCamera, mImagePathResult);

                Log.e("xbc", "degree:" + BitmapUtils.getImageDegree(mImagePathResult));
                if (mCrop) {// 裁剪
                    takingByCrop(mImagePathResult);//进行裁剪
                } else if (mImagePickCallBack != null) {
                    mImagePickCallBack.onSuccess(Uri.fromFile(new File(mImagePathResult)), mImagePathResult);
                } else {
                    Toast.makeText(mActivity, "图片获取失败", Toast.LENGTH_LONG).show();
                    if (mImagePickCallBack != null) {
                        mImagePickCallBack.onFailed();
                    }
                }
                break;
            case REQ_CODE_FROM_GALLERY://data.getData()返回的是选中的图片原始uri,我们需要存到自己的uri中
                if (data != null && data.getData() != null) {//成功
                    //data content://com.miui.gallery.open/raw//storage/emulated/0/DCIM/Camera/IMG_20190424_192129.jpg

                    // 7.0及以上需要将data转为path再转为galleryUri
                    //path /storage/emulated/0/DCIM/Camera/IMG_20190424_192129.jpg
                    mImagePathGallery = BitmapUtils.getRealPathFromUri(mActivity, data.getData());
                    Log.e("xbc", "uri:" + data.getData() + ",realPath:" + mImagePathGallery);

                    if (mCrop) {
                        takingByCrop(mImagePathGallery);//进行裁剪
                    } else if (mImagePickCallBack != null) {//根据返回uri取路径
                        saveImage(mImagePathGallery, mImagePathResult);//这里进行了采样率,旋转和体积压缩
                        mImagePickCallBack.onSuccess(Uri.fromFile(new File(mImagePathResult)), mImagePathResult);
                    }
                } else {
                    Toast.makeText(mActivity, "图片获取失败", Toast.LENGTH_LONG).show();
                    if (mImagePickCallBack != null) {
                        mImagePickCallBack.onFailed();
                    }
                }
                break;
            case REQ_CODE_FROM_ZOOM: //RESULT_OK即表示成功,因为我们指定了output
                if (mImagePickCallBack != null) {
                    Log.e("xbc", "result degree:" + BitmapUtils.getImageDegree(mImagePathResult));
                    Log.e("xbc", "crop degree:" + BitmapUtils.getImageDegree(mImagePathCrop));
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


