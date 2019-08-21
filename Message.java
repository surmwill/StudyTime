package com.example.st;

import com.google.firebase.database.ServerValue;

import java.util.Date;
import java.util.Map;

public class Message {
    private String sender;
    private String message;
    private String timeStamp;

    public Message(final String sender, final String message, final String timeStamp) {
        this.sender = sender;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getDate() { return timeStamp; }
}
