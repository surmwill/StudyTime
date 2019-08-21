package com.example.st.adapters;

import android.support.annotation.NonNull;

public class GroupInfo implements Comparable <GroupInfo> {
    private String key;
    private String name;
    private String studying;
    private String building;
    private String description;
    private String status;
    private String leaderKey;
    private long size;
    private long timestamp;

    public GroupInfo(String key, String name, String studying, String building, String description, String status, String leaderKey, long size, long timestamp) {
        this.key = key;
        this.name = name;
        this.studying = studying;
        this.building = building;
        this.description = description;
        this.status = status;
        this.leaderKey = leaderKey;
        this.size = size;
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getStudying() {
        return studying;
    }

    public String getBuilding() {
        return building;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getLeaderKey() {
        return leaderKey;
    }

    public long getSize() { return size; }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(@NonNull GroupInfo groupInfo) {
        if(timestamp > groupInfo.timestamp) return 1;
        else if(timestamp < groupInfo.timestamp) return -1;
        else return 0;
    }
}
