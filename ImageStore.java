package com.example.st;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.st.UserProfile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.annotation.Target;

import static com.example.st.MyUtils.ONE_MEGABYTE;

public class ImageStore {
    private String userKey;
    private StorageReference profileSTReference;
    private ImageOperationListener onImageOpCompleteListener;

    // Max profile image size
    private final long MAX_SIZE = 1 * ONE_MEGABYTE;

    ImageStore(final String userKey, @Nullable ImageOperationListener onImageOpCompleteListener) {
        this.userKey = userKey;
        profileSTReference = FirebaseStorage.getInstance().getReference().child(userKey);
        this.onImageOpCompleteListener = onImageOpCompleteListener;
    }

    public void removeOnImageOpCompleteListener() {
        onImageOpCompleteListener = null;
    }

    // Upload the image the user chose for their profile into database storage, this is the last step
    // of updating the profile so we also give a general profile update status message when this
    // completes.
    public void uploadImage(Uri filePath) {
        if(filePath != null) {
            // If the file size is too big we don't allow it in storage
            /*
            long fileSize = getImageSize(activity, filePath);
            if(fileSize > MAX_SIZE) {
                Toast.makeText(activity, "ERROR: profile image file size is too big: "
                        + Long.toString(MAX_SIZE) + "MB maximum", Toast.LENGTH_LONG).show();
                return;
            }
            */

            // Otherwise upload it and display a progress bar while it's uploading
            profileSTReference.child(UserProfile.STORAGE_VALUE_PROFILE_IMAGE).putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            if(onImageOpCompleteListener != null) onImageOpCompleteListener.imageUploaded(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if(onImageOpCompleteListener != null) onImageOpCompleteListener.imageUploaded(false);
                        }
                    });
        }
    }

    // Sets our profile image to one found in storage, display a progress bar while it's loading
    /* public void fetchProfileImage(final Context context, @Nullable final ImageView image) {
        profileSTReference.child(UserProfile.STORAGE_VALUE_PROFILE_IMAGE).getBytes(MAX_SIZE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Glide.with(context).load(bytes).into(image);
                        //if(image != null) image.setImageBitmap(bitmap);
                        onImageOpCompleteListener.ImageFetched(true, bitmap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        onImageOpCompleteListener.ImageFetched(false, null);
                    }
                });
    }
    */

    public void fetchProfileImage(final Context context) {
        profileSTReference.child(UserProfile.STORAGE_VALUE_PROFILE_IMAGE).getDownloadUrl()
        .addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();

                Glide.with(context).asBitmap().load(url).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if(onImageOpCompleteListener != null) onImageOpCompleteListener.imageFetched(true, resource);
                    }
                });
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(onImageOpCompleteListener != null) onImageOpCompleteListener.imageFetched(false, null);
            }
        });
    }

    // Returns the image size in bytes of the given filePath(a URI)
    public static long getImageSize(Activity activity, Uri filePath) {
        Cursor returnCursor = activity.getContentResolver().query(filePath, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        return returnCursor.getLong(sizeIndex);
    }

    public static boolean hasImage(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        boolean hasImage = (drawable != null);

        if(hasImage && (drawable instanceof BitmapDrawable))
            hasImage = ((BitmapDrawable)drawable).getBitmap() != null;

        return hasImage;
    }

    interface ImageOperationListener {
        void imageFetched(boolean success, @Nullable Bitmap imageBitmap);
        void imageUploaded(boolean success);
    }
}
