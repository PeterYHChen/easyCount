package com.example.peter.berryestimator;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * Created by yonghong on 4/30/17.
 * Used to maintained image data during app orientaiton changes
 */

public class ImageDataFragment extends Fragment {

    // data object we want to retain
    private Bitmap mImage = null;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setImage(Bitmap image) {
        this.mImage = image;
    }

    public Bitmap getImage() {
        return mImage;
    }
}
