package com.example.peter.berryestimator;

/**
 * Created by yonghong on 4/25/17.
 */

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.ImageView;
import android.widget.Toast;

public class EditImageDialogFragment extends DialogFragment {
    private static final String IMAGE_PATH = "image_path";
    private String mImagePath;
    private DBManager dbManager;
    private OnEditImageFragmentInteractionListener mListener;

    DragCircleView mDragCircleView;

    public static EditImageDialogFragment newInstance(String imagePath) {
        EditImageDialogFragment fragment = new EditImageDialogFragment();

        Bundle args = new Bundle();
        args.putString(IMAGE_PATH, imagePath);
        fragment.setArguments(args);

        return fragment;
    }

    //    Mandatory empty constructor for the fragment manager to instantiate the
//    fragment (e.g. upon screen orientation changes).
    public EditImageDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
        mImagePath = getArguments().getString(IMAGE_PATH);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_image, container, false);

        final ImageButton closeButton = (ImageButton)view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mDragCircleView = (DragCircleView) view.findViewById(R.id.drag_circle_view);
        if (mDragCircleView != null) {
            mDragCircleView.setOnUpCallback(new DragCircleView.OnUpCallback() {
                @Override
                public void onRectFinished(final Rect rect) {
                    Toast.makeText(getActivity(), "Rect is (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        Bitmap image = MyUtils.getResizedImage(getActivity(), mImagePath);
        mDragCircleView.setImageBitmap(image);
//
        final Button saveButton = (Button)view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCroppedImage(mDragCircleView.getCroppedCircleBitmap(), mImagePath);
                    dismiss();
                }
            }
        });
        return view;
    }
//
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu, inflater);
//        menu.clear();
//        inflater.inflate(R.menu.menu_edit_image_fragment, menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        //send edited image back
//        if (id == R.id.action_save) {
//            mListener.onCroppedImage(mDragCircleView.getCroppedCircleBitmap());
//            dismiss();
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("----------", "on attach");
        try {
            mListener = (OnEditImageFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEditImageFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnEditImageFragmentInteractionListener {
        void onCroppedImage(Bitmap image, String imagePath);
    }
}
