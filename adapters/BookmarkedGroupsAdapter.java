package com.example.st.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.st.GroupManager;
import com.example.st.MyUtils;
import com.example.st.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BookmarkedGroupsAdapter extends RecyclerView.Adapter<BookmarkedGroupsAdapter.ViewHolder> {
    private Context mContext;
    private List<GroupInfo> groupInfos;
    private LayoutInflater mInflater;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupName;
        private final TextView groupStudying;
        private final TextView groupBuilding;
        private final TextView groupDescription;

        public ViewHolder (View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.bookmarkedGroupsName);
            groupStudying = itemView.findViewById(R.id.bookmarkedGroupsStudying);
            groupBuilding = itemView.findViewById(R.id.bookmarkedGroupsBuilding);
            groupDescription = itemView.findViewById(R.id.bookmarkedGroupsDescription);
        }

        // Groups -> groupkey
        public void bind(GroupInfo groupInfo, Context context) {
            final String name = groupInfo.getName();
            final String studying = groupInfo.getStudying();
            final String building = groupInfo.getBuilding();
            final String description = groupInfo.getDescription();
            final String groupStatus = groupInfo.getStatus();

            if(groupStatus.equals(GroupManager.STATUS_GROUP_OPEN))
                groupName.setTextColor(ContextCompat.getColor(context, R.color.online));
            else
                groupName.setTextColor(ContextCompat.getColor(context, R.color.offline));

            groupName.setText(name);
            groupStudying.setText(studying);
            groupBuilding.setText(building);
            groupDescription.setText(description);
        }
    }

    public BookmarkedGroupsAdapter(Context context, List<GroupInfo> groupInfos) {
        this.mInflater = LayoutInflater.from(context);
        this.groupInfos = groupInfos;
        mContext = context;
    }

    @Override
    public BookmarkedGroupsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.row_bookmarked_groups, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.bind(groupInfos.get(position), mContext);
    }

    @Override
    public int getItemCount() {
        return groupInfos.size();
    }

    public GroupInfo getGroupInfo(int index) {
        return groupInfos.get(index);
    }

    public GroupInfo removeGroup(int index) {
        GroupInfo groupInfo = groupInfos.remove(index);
        notifyItemRemoved(index);
        return groupInfo;
    }

    public void restoreGroupKey(int index, GroupInfo groupInfo) {
        groupInfos.add(index, groupInfo);
        notifyItemInserted(index);
    }
}
