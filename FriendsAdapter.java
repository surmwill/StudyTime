package com.example.st;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
    private Context mContext;
    private LayoutInflater mInflater;
    private List<FriendInfo> mFriendInfoList;
    private static MemberProfileView.LaunchMemberProfileCallback launchFriendProfileCallback;

    // used to remove callbacks for when we remove the adapter
    private Set<ViewHolder> boundVHs;

    public static void setLaunchFriendProfileCallback(Context context) {
        try {
            launchFriendProfileCallback = (MemberProfileView.LaunchMemberProfileCallback) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException("FriendsAdapter must implement MemberProfileView.LaunchMemberProfileCallback");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements ImageStore.ImageOperationListener {
        private TextView friendName;
        private TextView friendMajor;
        private TextView friendStatus;
        private ImageView friendProfileImage;

        private FriendInfo mFriendInfo;
        private ConstraintLayout friendCLayout;
        private ImageStore imageStore;

        //private ProgressBar imagePBar;

        public ViewHolder(View itemView) {
            super(itemView);

            // ImageView
            friendProfileImage = itemView.findViewById(R.id.friendOverviewProfileImage);

            // ProgressBar
            //imagePBar = itemView.findViewById(R.id.friendOverviewProgressBar);

            // TextView
            friendName = itemView.findViewById(R.id.friendOverviewName);
            friendMajor = itemView.findViewById(R.id.friendOverviewMajor);
            friendStatus = itemView.findViewById(R.id.friendOverviewStatus);

            friendCLayout = itemView.findViewById(R.id.friendOverviewRowCLayout);
            friendCLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mFriendInfo != null) launchFriendProfileCallback.launchMemberProfileFrag(mFriendInfo.getKey(), mFriendInfo.getProfileBitmap(), false);
                }
            });

        }

        // UserProfile -> userkey
        public void bind(Context context, FriendInfo friendInfo) {
            mFriendInfo = friendInfo;

            final String name = mFriendInfo.getName();
            final String major = mFriendInfo.getMajor();
            final String key = mFriendInfo.getKey();
            final String onlineStatus = mFriendInfo.getOnlineStatus();
            final String groupStatus = mFriendInfo.getGroupStatus();
            final String status;

            if(friendInfo.getProfileBitmap() == null) {
                imageStore = new ImageStore(key, this);
                imageStore.fetchProfileImage(context);
            }
            else friendProfileImage.setImageBitmap(friendInfo.getProfileBitmap());

            if(onlineStatus.equals(UserProfile.STATUS_ONLINE))
                friendName.setTextColor(ContextCompat.getColor(context, R.color.online));
            else
                friendName.setTextColor(ContextCompat.getColor(context, R.color.offline));

            if(friendInfo.isFriendRequest()) {
                status = "Pending Friend Request";
                friendStatus.setTextColor(ContextCompat.getColor(context, R.color.gold));
            }
            else if(groupStatus.equals(UserProfile.STATUS_NOT_IN_GROUP)) {
                status = "Not In Group";
                friendStatus.setTextColor(ContextCompat.getColor(context, R.color.online));
            }
            else {
                status = "In Group";
                friendStatus.setTextColor(ContextCompat.getColor(context, R.color.offline));
            }

            friendName.setText(name);
            friendMajor.setText(major);
            friendStatus.setText(status);
        }

        public void removeImageOpListener() {
            if(imageStore != null) imageStore.removeOnImageOpCompleteListener();
        }



        @Override
        public void imageFetched(boolean success, @Nullable Bitmap imageBitmap) {
            if(success) {
                mFriendInfo.setProfileBitmap(imageBitmap);
                friendProfileImage.setImageBitmap(imageBitmap);
            }
        }

        @Override
        public void imageUploaded(boolean success) {

        }
    }

    public FriendsAdapter(Context context, ArrayList <FriendInfo> friendInfoList) {
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mFriendInfoList = friendInfoList;
        boundVHs = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_friends_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final FriendInfo friendInfo = mFriendInfoList.get(position);
        holder.bind(mContext, friendInfo);
        boundVHs.add(holder);
    }

    // Called after onViewDetachedFromWindow and before the ViewHolder is sent to the RecyclerViewPool;
    // a "cleanup" for onBindViewHolder. Note that before this is called onViewAttachedFromWindow can
    // be called without needing to rebind data. Detach the callback here since we have no guarantee
    // the ViewHolder is bound to the same friend.
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.removeImageOpListener();
        boundVHs.remove(holder);
    }

    // Called when we stop observing the current adapter. Detach all callbacks since the ViewHolders
    // will no longer exist
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for(final ViewHolder vh : boundVHs) {
            vh.removeImageOpListener();
        }
    }

    @Override
    public int getItemCount() {
        return mFriendInfoList.size();
    }

    public static class FriendInfo implements Comparable <FriendInfo> {
        final private String key;
        final private String name;
        final private String major;
        final private String groupStatus;
        final private String onlineStatus;
        final private boolean isFriendRequest;

        private Bitmap profileBitmap;

        FriendInfo(String key, String name, String major, String groupStatus, String onlineStatus, boolean isFriendRequest) {
            this.key = key;
            this.name = name;
            this.major = major;
            this.groupStatus = groupStatus;
            this.onlineStatus = onlineStatus;
            this.isFriendRequest = isFriendRequest;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getMajor() {
            return major;
        }

        public String getGroupStatus() {
            return groupStatus;
        }

        public String getOnlineStatus() {
            return onlineStatus;
        }

        public Bitmap getProfileBitmap() {
            return profileBitmap;
        }

        public void setProfileBitmap(Bitmap profileBitmap) {
            this.profileBitmap = profileBitmap;
        }

        public boolean isFriendRequest() {
            return isFriendRequest;
        }

        @Override
        public int compareTo(@NonNull FriendInfo friendInfo) {
            return name.compareToIgnoreCase(friendInfo.name);
        }

    }

}
