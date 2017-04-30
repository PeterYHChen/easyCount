package com.example.peter.berryestimator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gc.materialdesign.widgets.Dialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.sql.StatementEvent;

/**
 * Created by peter on 26/05/15.
 *
 */
public class MyUtils {

    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    public static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 2;

    // change imageview width of imageView in @activity_create_image_record.xml when change this value
    private static final int resizedImageSize = 500;
    private static long timelog;

    public static Intent getConfiguredGalleryIntent() {
        // invoke gallery intent
        Intent galleryIntent = null;

        // if build version is API KitKat 19 and up, try to get persisted permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            // build version is lower
            galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        galleryIntent.setType("image/*");
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        return galleryIntent;
    }

    public static String getMd5(final String toEncrypt) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(toEncrypt.getBytes());
            final byte[] bytes = digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString().toLowerCase();
        } catch (Exception exc) {
            return "";
        }
    }

    // get absolute path to avoid permission problem with media content provider
    public static String getAbsImagePath(Context ct, Uri uri){
        String document_id, path;
        Cursor cursor = ct.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
            cursor.close();

            cursor = ct.getContentResolver().query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            if (cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();

                return "file://" + path;
            }
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    public static long getLastModifiedDate(String imagePath) {
        // path string has to be parse into Uri and turn back to string to get a correct format path
        File file = new File(Uri.parse(imagePath).getPath());
        if (file.exists()) {
            return file.lastModified();
        }

        return -1;
    }

    public static boolean imagePathIsValid(String imagePath) {
        return imagePath != null && !imagePath.isEmpty() && imageExists(imagePath);
    }

    public static boolean imageExists(String imagePath) {
        // path string has to be parse into Uri and turn back to string to get a correct format path
        File file = new File(Uri.parse(imagePath).getPath());
        return file.exists();
    }

    public static String getCompressedImageString(Context ct, String imagePath) {
        return compressImageToString(getResizedImage(ct, imagePath));
    }

    public static String getCompressedImageString(Bitmap image) {
        return compressImageToString(getResizedImage(image, resizedImageSize));
    }

    public static Bitmap getImageFromPath(Context ct, String imagePath) {
        InputStream stream = null;
        Bitmap bm = null;
        try {
            stream = ct.getContentResolver().openInputStream(Uri.parse(imagePath));
            bm = BitmapFactory.decodeStream(stream);

            if (bm != null) {
                Log.d("image original size", bm.getByteCount()/1024 + "KB");
                Log.d("------", "width: " + bm.getWidth() + " height: " + bm.getHeight());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }

    // resized image min edge to be 500
    public static Bitmap getResizedImage(Context ct, String imagePath) {
        InputStream stream = null;
        Bitmap image = getImageFromPath(ct, imagePath);
        image = getResizedImage(image, resizedImageSize);
        return image;
    }

    public static String compressImageToString(Bitmap bm){
        if (bm == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap decodeBitmapFromString(String imageBase64){
        if (imageBase64 == null)
            return null;

        byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // return mutable bitmap
        if (bmp != null)
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

        return bmp;
    }

    // resize bitmap based on the shortest side
    public static Bitmap getResizedImage(Bitmap bm, int newMinSide) {
        if (bm == null)
            return null;

        int width = bm.getWidth();
        int height = bm.getHeight();

        // do not resize when it is smaller than the target bitmap
        int minSide = width < height ? width : height;
        if (minSide < newMinSide) {
            return bm;
        }

        float scaleRatio = ((float) newMinSide) / minSide;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleRatio, scaleRatio);

        // "RECREATE" THE NEW BITMAP
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    public static Bitmap transformSquareToCircleBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap,
                BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return bitmap;
    }

    // use CIE76 ΔE*ab to compute color similarity
    // reuturn a double value in [0, 1], 0 means no difference
    public static boolean colorsAreSimilar(int a, int b, int maxDelta) {
        int redDiff = Color.red(a) - Color.red(b);
        int blueDiff = Color.blue(a) - Color.blue(b);
        int greenDiff = Color.green(a) - Color.green(b);

        double deltaE = Math.sqrt(2*redDiff*redDiff + 4*blueDiff*blueDiff + 3*greenDiff*greenDiff);
//        Log.d("--", "deltaE: " + deltaE);
        return deltaE < maxDelta;
    }

    public static String getTimeString(long time) {
        SimpleDateFormat format = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return format.format(new Date(time));
    }

    public static String getDateString(long time, boolean withYear) {
        SimpleDateFormat format;

        if (withYear) {
            format = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        } else {
            format = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        }

        return format.format(new Date(time));
    }

    public static String getProperTimeString(long time) {
        Calendar preCalendar = new GregorianCalendar(TimeZone.getDefault());
        Calendar currCalendar = new GregorianCalendar(TimeZone.getDefault());
        preCalendar.setTimeInMillis(time);
        currCalendar.setTimeInMillis(System.currentTimeMillis());

        if (preCalendar.get(Calendar.DATE) == currCalendar.get(Calendar.DATE)) {
            return getTimeString(time);
        } else if (preCalendar.get(Calendar.YEAR) == currCalendar.get(Calendar.YEAR)) {
            return getDateString(time, false);
        } else {
            return getDateString(time, true);
        }
    }

    public static String getTimePassedString(long createdDate) {
        long timePassed = (System.currentTimeMillis() - createdDate) / 1000;

        long[] timeUnit = {60, 60, 24, 30, 12};
        String[] dateUnit = {" secs ", " mins ", " hours ", " days ", " months ", " years "};

        int i;
        for (i = 0; i < timeUnit.length; i++) {
            if (timePassed/timeUnit[i] < 1) {
                break;
            }
            timePassed /= timeUnit[i];
        }

        return timePassed + dateUnit[i] + "ago";
    }

    public static void startTimelog() {
        timelog = System.currentTimeMillis();
    }

    public static void endTimelog(String logMsg) {
        Log.d("------", logMsg + " time cost: " + (System.currentTimeMillis() - timelog));
    }

    public static void logImageRecord(ImageRecord imageRecord){
        Log.d("", "--------------------------------");
        Log.d("record id", imageRecord.getRecordId());
        Log.d("image title", imageRecord.getTitle());
        Log.d("create date", new Date(imageRecord.getCreatedDate()).toString());
        Log.d("image path", imageRecord.getImagePath());
        Log.d("estimate", "" + imageRecord.getEstimate());
    }

    public void showTempInfo(Context ct, String s){
        Toast.makeText(ct, s, Toast.LENGTH_SHORT).show();
    }
//    public ArrayList<ImageRecord> readDataFromFile(File file){
//
//        GsonBuilder gsonBuilder = new GsonBuilder();
//        Gson gson = gsonBuilder.create();
//
//        if (file == null){
//            return null;
//        }
//
//        try {
//            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
//            byte[] bytes = new byte[(int)file.length()];
//
//            Log.d("---------", "read data from file");
//            // read the whole file
//            if (inputStream.read(bytes) == file.length()){
//                inputStream.close();
//
//                Log.d("---------", "finish read data from file");
//
//                Type collectionType = new TypeToken<ArrayList<ImageRecord>>() {}.getType();
//                return gson.fromJson(new String(bytes, "UTF-8"), collectionType);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

//    public void writeDataToFile(ArrayList<ImageRecord> imageRecordList, File file){
//
//        GsonBuilder gsonBuilder = new GsonBuilder();
//        Gson gson = gsonBuilder.create();
//
//        if (file == null){
//            return;
//        }
//
//        OutputStream outputStream = null;
//        try {
//            Log.d("---------", "wrtie data to file");
//            outputStream = new BufferedOutputStream(new FileOutputStream(file));
//            outputStream.write(gson.toJson(imageRecordList).getBytes("UTF-8"));
//
//            Log.d("---------", "finish wrtie data to file");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (outputStream != null){
//                try {
//                    outputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public File getOutputDataFile(){
//        // check that the SDCard is mounted
//        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
//            File dataStorageDir = new File(Environment.getExternalStorageDirectory(), "berry_estimator");
//
//            // Create the storage directory if it does not exist
//            if (! dataStorageDir.exists()){
//                if (! dataStorageDir.mkdirs()){
//                    showTempInfo("Failed to create directory");
//                    return null;
//                }
//            }
//
//            // Create the initial file name
//            String fileName = "image_record_list";
//
//            File dataFile = new File(dataStorageDir.getPath(), fileName + ".dat");
//            if (!dataFile.exists()) {
//                try {
//                    Log.d("---------", "create new data file");
//                    dataFile.createNewFile();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            return dataFile;
//
//        } else {
//            Log.d("MyThesisApp", "SD card state: " + Environment.getExternalStorageState());
//            showTempInfo("SD card not mounted");
//            return null;
//        }
//    }
}
