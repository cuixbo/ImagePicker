package com.cuixbo.lib.imagepicker;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.media.ExifInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class BitmapUtils {

    /**
     * 将srcPath图片转成bitmap，采样率缩放处理后，compress输出到destPath文件
     *
     * @param context
     * @param srcPath
     * @param destPath
     * @return
     */
    public static boolean saveInSampleToFile(Context context, String srcPath, String destPath) {
        if (srcPath == null || destPath == null) {
            return false;
        }
        //从uri获取bitmap(采样率缩放)
        Bitmap bitmap = getInSampleBitmapFromUri(context, Uri.fromFile(new File(srcPath)));
        //compress
        return compressBitmapToFile(bitmap, destPath, 100);
    }


    /**
     * 从Uri获取Bitmap，返回经过采样处理的图片
     *
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap getInSampleBitmapFromUri(Context context, Uri uri) {
        Bitmap bitmap = null;
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            // 采样
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            options.inSampleSize = calculateInSampleSize(options, 720, 1280);//设置缩放比例
            options.inJustDecodeBounds = false;
            in = context.getContentResolver().openInputStream(uri);
            // 根据采样，获取图片
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }


    /**
     * 计算采样率
     *
     * @param options
     * @param width
     * @param height
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        int inSampleSize = 1;//1表示不缩放
        if (outWidth > outHeight) {//宽度大
            if (outWidth > height) {//宽度超过了1280，需要压缩
                int r = (int) (outWidth * 1F / height);
                inSampleSize = (int) Math.pow(2, (int) (Math.ceil(Math.log(r) / Math.log(2))));
            }
        } else {//高度大
            if (outHeight > height) {//高度超过了1280，需要压缩
                int r = (int) (outHeight * 1F / height);
                inSampleSize = (int) Math.pow(2, (int) (Math.ceil(Math.log(r) / Math.log(2))));
            }
        }
        return inSampleSize;
    }


    /**
     * 压缩bitmap输出到文件
     *
     * @param bitmap
     * @param imagePath
     * @return
     */
    public static boolean compressBitmapToFile(Bitmap bitmap, String imagePath, int quality) {
        if (bitmap == null || imagePath == null) {
            return false;
        }
        FileOutputStream fos = null;
        boolean isCompressed = false;
        try {
            fos = new FileOutputStream(new File(imagePath));
            isCompressed = bitmap.compress(CompressFormat.JPEG, quality, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isCompressed;
    }


    /**
     * 从图片uri中读取图片路径
     *
     * @param uri
     * @return
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String path = null;
        try {
            final String scheme = uri.getScheme();
            if (scheme == null) {
                path = uri.getPath();
            } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                path = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                Cursor cursor = context.getContentResolver().query(uri, new String[]{ImageColumns.DATA}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(ImageColumns.DATA);
                        if (index > -1) {
                            path = cursor.getString(index);
                        }
                    }
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * 从图片路径中获取图片旋转的角度
     *
     * @param imagePath
     * @return
     */
    public static int getImageDegree(String imagePath) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转Bitmap图片
     *
     * @param bitmap
     * @param angle
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = new Matrix();// 旋转图片 动作
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotatedBitmap;
    }

}
