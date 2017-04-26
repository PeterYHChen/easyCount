package com.example.peter.berryestimator;

/**
 * Created by yonghong on 4/25/17.
 */

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import android.widget.ImageView;
import android.widget.Toast;

public class EditImageDialogFragment extends DialogFragment {
    private static final String IMAGE_RECORD = "image_record";
    private ImageRecord imageRecord;
    private DBManager dbManager;

    public static EditImageDialogFragment newInstance(ImageRecord imageRecord) {
        EditImageDialogFragment fragment = new EditImageDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(IMAGE_RECORD, imageRecord);
        fragment.setArguments(args);

        return fragment;
    }

    //    Mandatory empty constructor for the fragment manager to instantiate the
//    fragment (e.g. upon screen orientation changes).
    public EditImageDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MyUtils.startTimelog();

        super.onCreate(savedInstanceState);
        dbManager = new DBManager(getActivity());

        imageRecord = getArguments().getParcelable(IMAGE_RECORD);

        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_image, container, false);

        ImageButton closeButton = (ImageButton)view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Bitmap image = MyUtils.getResizedImage(getActivity(), imageRecord.getImagePath());

//        final ImageView imageView = (ImageView)view.findViewById(R.id.image_view);
        final DragCircleView dragCircleView = (DragCircleView) view.findViewById(R.id.drag_circle_view);

        if (dragCircleView != null) {
            dragCircleView.setOnUpCallback(new DragCircleView.OnUpCallback() {
                @Override
                public void onRectFinished(final Rect rect) {
                    Toast.makeText(getActivity(), "Rect is (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        dragCircleView.setImageBitmap(image);


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.closeDB();
    }

    public interface OnEditImageFragmentInteractionListener {
        void onEditedImage(ImageRecord imageRecord);
    }
}
