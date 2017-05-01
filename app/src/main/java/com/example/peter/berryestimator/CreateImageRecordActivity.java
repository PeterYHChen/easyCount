package com.example.peter.berryestimator;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.gc.materialdesign.widgets.Dialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class CreateImageRecordActivity extends ActionBarActivity
        implements EditImageDialogFragment.OnEditImageFragmentInteractionListener {
    public static final String TAG_EDIT_IMAGE_DIALOG_FRAGMENT = "edit_image_dialog_fragment";
    public static final String IMAGE_RECORD = "image_record";
    public static final String EDITING_IMAGE_PATH = "editing_image_path";
    public static final String IMAGE_DATA = "image_data";
    public static final String IMAGE_RECORD_POSITION = "image_record_position";
    public static final String IMAGE_RECORD_ACTION = "image_record_action";
    public static final int IMAGE_RECORD_ACTION_NOT_FOUND = -1;
    public static final int IMAGE_RECORD_CREATE = 0;
    public static final int IMAGE_RECORD_EDIT = 1;
    public static final int IMAGE_RECORD_REMOVE = 2;
    public static final String NO_TITLE = "(No title)";

    private DBManager mDBManager;

    private ImageRecord mImageRecord;
    private int mAction;

    // this image path is used to indicate whether an image is selected to edit. null means no image is selected.
    // this image path will not be saved in mImageRecord until the image is cropped successfully.
    // used to handle screen rotation problem
    private String mEditingImagePath;
    private ImageDataFragment mImageFragment;

    private static long timelog;

    private ImageView mImageView;
    private Spinner targetTypeSpinner;
    private EditText imageLocationEditText;
    private EditText titleEditText;
    private EditText actualCountEditText;

//    private DisplayImageOptions mImageOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_image_record);

        mDBManager = new DBManager(this);

        // read action
        mAction = getIntent().getIntExtra(IMAGE_RECORD_ACTION, IMAGE_RECORD_ACTION_NOT_FOUND);

        if (savedInstanceState == null) {
            // read image record data
            mImageRecord = getIntent().getParcelableExtra(IMAGE_RECORD);
            // Empty string is used only for the first time
            mEditingImagePath = "";
        } else {
            // used pre-saved data
            mImageRecord = savedInstanceState.getParcelable(IMAGE_RECORD);
            mEditingImagePath = savedInstanceState.getString(EDITING_IMAGE_PATH);
        }

        FragmentManager fm = getFragmentManager();
        mImageFragment = (ImageDataFragment) fm.findFragmentByTag(IMAGE_DATA);
        // create the fragment and get original image from device for the first time
        if (mImageFragment == null) {
            // add the fragment
            mImageFragment = new ImageDataFragment();
            fm.beginTransaction().add(mImageFragment, IMAGE_DATA).commit();

            // only init with original image when action is not create action
            if (mAction != IMAGE_RECORD_CREATE)
                mImageFragment.setImage(getOriginalImageFromImageRecord());
        }

        // initiate and config imageloader
        initImageLoader();

        mImageView = (ImageView) findViewById(R.id.record_image);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditImageDialog(mImageRecord.getImagePath());
            }
        });

        // display image of current record
        displayImageInImageView(mImageFragment.getImage());

        // set text fields and spinners
        titleEditText = (EditText) findViewById(R.id.record_title);
        targetTypeSpinner = (Spinner) findViewById(R.id.record_target_type);
        imageLocationEditText = (EditText) findViewById(R.id.record_image_location);
        actualCountEditText = (EditText) findViewById(R.id.record_actual_count);

        Button pickImageButton = (Button) findViewById(R.id.pick_image_button);
        Button deleteButton = (Button) findViewById(R.id.delete_button);

        pickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(MyUtils.getConfiguredGalleryIntent(), MyUtils.PICK_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecordRemoveDialog();
            }
        });

        final ArrayAdapter<CharSequence> targetTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.object_type_array, android.R.layout.simple_spinner_item);
        targetTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetTypeSpinner.setAdapter(targetTypeAdapter);

        // if create record event, set default value
        if(mAction == IMAGE_RECORD_CREATE){
            targetTypeSpinner.setSelection(0);
            deleteButton.setVisibility(View.GONE);

            // switch to edit image fragment if the editing image path is not null
            if (mEditingImagePath != null && mEditingImagePath.isEmpty())
                showEditImageDialog(mImageRecord.getImagePath());
            else if (mEditingImagePath != null)
                showEditImageDialog(mEditingImagePath);

        // if edit record event, display data from image record
        } else {
            // check if target type exists
            int pos = targetTypeAdapter.getPosition(mImageRecord.getTargetType());
            if (pos >= 0)
                targetTypeSpinner.setSelection(pos);
            else
                targetTypeSpinner.setSelection(0);


            titleEditText.setText(mImageRecord.getTitle().equals(NO_TITLE) ? "" : mImageRecord.getTitle());
            imageLocationEditText.setText(mImageRecord.getImageLocation());
            if (mImageRecord.getActualCount() >= 0) {
                actualCountEditText.setText(String.valueOf(mImageRecord.getActualCount()));
            }
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void showRecordRemoveDialog() {
        // com.gc.materialdesign.widgets
        final Dialog recordRemoveDialog = new Dialog(this, "Warning", "Do you want to delete this record?");
        recordRemoveDialog.addCancelButton("CANCEL");
        recordRemoveDialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReturnData(IMAGE_RECORD_REMOVE);
            }
        });

        recordRemoveDialog.setOnCancelButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordRemoveDialog.dismiss();
            }
        });
        recordRemoveDialog.show();
    }

    private void initImageLoader() {
        if(!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = ImageLoaderConfiguration.createDefault(getApplicationContext());
            ImageLoader.getInstance().init(config);
        }
    }

    private Bitmap getOriginalImageFromImageRecord() {
        Bitmap image = null;
        if (!mImageRecord.getRecordId().isEmpty()) {
            Log.d("Image retreved from", "database");
            Cursor cursor = mDBManager.findRowCursor(mImageRecord);
            if (cursor != null && cursor.moveToFirst()) {
                String imageString = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_IMAGE));
                image = MyUtils.decodeBitmapFromString(imageString);
            }
            if (cursor != null)
                cursor.close();
        } else if (MyUtils.imagePathIsValid(mImageRecord.getImagePath())){
            // only display image when image path is valid
            Log.d("Image retreved from", "image path" + mImageRecord.getImagePath());
            image = MyUtils.getImageFromPath(this, mImageRecord.getImagePath());
            // TODO: this function might reduce delay?
//            ImageLoader.getInstance().displayImage(mImageRecord.getImagePath(), mImageView);
        } else {
            showTempInfo("The original image is no more on current device, " +
                    "please select another image to evaluate");
        }

        return image;
    }

    // display image in mImageView
    private void displayImageInImageView(Bitmap image) {
        mImageFragment.setImage(image);
        mImageView.setImageBitmap(image);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MyUtils.PICK_IMAGE_ACTIVITY_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    // transform content path into absolute path and switch to edit image fragment
                    Uri imageUri = data.getData();
                    String imagePath = MyUtils.getAbsImagePath(this, imageUri);
                    showEditImageDialog(imagePath);
                } else {
                    Log.e("------", "Pick image from gallery error");
                }
            }
        }
    }

    public void showEditImageDialog(String imagePath) {
        // if the image path does not retreive image, skip showing this dialog
        if (!MyUtils.imagePathIsValid(imagePath)) {
            showTempInfo("The original image is no more on current device, please select another image");
            return;
        }
        // the image path is to be edited, store it temporarily
        mEditingImagePath = imagePath;

        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_EDIT_IMAGE_DIALOG_FRAGMENT);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // skip if current dialog has shown up, avoid repeatedly showing the same fragment
        if (prev != null) {
            ft.show(prev);
            ft.commit();
        } else {
            EditImageDialogFragment editImageDialogFragment = EditImageDialogFragment.newInstance(imagePath);
            editImageDialogFragment.show(getSupportFragmentManager(), TAG_EDIT_IMAGE_DIALOG_FRAGMENT);
        }
    }

    @Override
    public void onCroppedImage(Bitmap image, String imagePath) {
        displayImageInImageView(image);
        mImageRecord.setImagePath(imagePath);
        mImageRecord.setImageTakenDate(MyUtils.getLastModifiedDate(imagePath));
        Log.d("Image retrieved from", mImageRecord.getImagePath());
        Log.d("Image last modified", mImageRecord.getImageTakenDate() + "");

        // set back to null indicating no image is editing
        mEditingImagePath = null;
    }

    @Override
    // when edit image fragment is cancelled, this function is called
    public void onCancelledImageEdition() {
        // if an image has been cropped and set, editImageDialogFragment only go back to createImageRecordActivity page.
        if (getDisplayedImage() != null)
            return;

        // if this is a action_record_create event, no image was set, then skip current activity and go back to MainTabActivity
        if (mAction == IMAGE_RECORD_CREATE) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(IMAGE_RECORD, mImageRecord);
        outState.putString(EDITING_IMAGE_PATH, mEditingImagePath);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mImageRecord = savedInstanceState.getParcelable(IMAGE_RECORD);
        mEditingImagePath = savedInstanceState.getString(EDITING_IMAGE_PATH);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDBManager.closeDB();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_image_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //save image record
        if (id == R.id.action_save) {
            // retrieve values
            // TODO: change file name of the image to be organized
            String text = titleEditText.getText().toString();
            mImageRecord.setTitle(text.isEmpty() ? NO_TITLE : text);

            mImageRecord.setTargetType(targetTypeSpinner.getAdapter().
                    getItem(targetTypeSpinner.getSelectedItemPosition()).toString());

            mImageRecord.setImageLocation(imageLocationEditText.getText().toString());

            // if count is empty, no need to save
            text = actualCountEditText.getText().toString();
            if (!text.isEmpty()) {
                mImageRecord.setActualCount(Integer.parseInt(text));
            }

            if (mImageRecord.getImagePath().isEmpty() || mImageFragment.getImage() == null) {
                showTempInfo("Please choose a picture");
                return true;
            }

//            if (!MyUtils.imageExists(mImageRecord.getImagePath())) {
//                showTempInfo("Image does not exist on device, please choose another picture");
//                return true;
//            }

            String compressedThumbnailString = getCompressedThumbnailString();
            if (compressedThumbnailString == null) {
                showTempInfo("Error. Empty thumbnail was returned");
                return true;
            }

            mImageRecord.setCompressedThumbnailString(compressedThumbnailString);
            Log.d("thumbnail length", mImageRecord.getCompressedThumbnailString().length() / 1024.0 + "KB");

            mImageRecord.setIsSynced(false);

            setReturnData(mAction);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // return data back to main tab activity
    private void setReturnData(int action){
        String imageString = null;

        switch (action){
            case IMAGE_RECORD_CREATE:
                Log.d("----------", "insert image record");
                // TODO: reduce time cost, too slow, can try new runnable to open another thread
                imageString = MyUtils.getCompressedImageString(getDisplayedImage());
                if (imageString == null) {
                    return;
                }

                mImageRecord.setRecordId(mDBManager.insert(mImageRecord, imageString));
                break;

            case IMAGE_RECORD_EDIT:
                Log.d("----------", "update image record");
                // TODO: reduce time cost, compression too slow, can try new runnable to open another thread
                imageString = MyUtils.getCompressedImageString(getDisplayedImage());
                if (imageString == null) {
                    return;
                }
                mImageRecord.setEstimate(-1);
                mDBManager.update(mImageRecord, imageString, "");
                break;

            case IMAGE_RECORD_REMOVE:
                Log.d("----------", "remove image record");
                mDBManager.delete(mImageRecord);
                break;

            default:
                Log.e("image record", "action not found");
                showTempInfo("fail to recognize action");
                return;
        }

        Intent returnIntent = new Intent();
        returnIntent.putExtra(IMAGE_RECORD, mImageRecord);
        returnIntent.putExtra(IMAGE_RECORD_ACTION, action);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private String getCompressedThumbnailString(String imagePath, int width) {
        // Set compressed thumbnail of image
        ImageSize imageSize = new ImageSize(width, width);
        Bitmap image = ImageLoader.getInstance().loadImageSync(imagePath, imageSize);

        image = ThumbnailUtils.extractThumbnail(image, width, width, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return MyUtils.compressImageToString(image);
    }

    private Bitmap getDisplayedImage() {
        return mImageFragment.getImage();
    }

    private String getCompressedThumbnailString() {
        int width = 300;
        int height = 300;
        Bitmap image = getDisplayedImage();

        // Set compressed thumbnail of image
        image = MyUtils.getResizedImage(image, width);
        image = ThumbnailUtils.extractThumbnail(image, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return MyUtils.compressImageToString(image);
    }

    private void showTempInfo(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

}
