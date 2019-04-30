# ImagePicker
[![](https://jitpack.io/v/cuixbo/ImagePicker.svg)](https://jitpack.io/#cuixbo/ImagePicker)

调用系统的相机、相册选取图片，调用系统的裁剪进行图片裁剪等功能获取图片。修复4.4、7.0的uri问题，修复旋转问题。

##### 1.兼容4.4及7.0的uri问题；
##### 2.处理了部分手机厂商拍照、相册、裁剪图片旋转的问题；
##### 3.处理了文件路径重复的问题；
封装成library，可以依赖使用。

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  implementation 'com.github.cuixbo:ImagePicker:1.0.1'
}
```
