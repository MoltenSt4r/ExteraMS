package com.radolyn.ayugram.database.entities;

import androidx.room.Entity;

@Entity(primaryKeys = {"userId", "dialogId", "messageId"})
public class SpyMessageRead {
    public long userId;
    public long dialogId;
    public int messageId;
    public int date;
}
