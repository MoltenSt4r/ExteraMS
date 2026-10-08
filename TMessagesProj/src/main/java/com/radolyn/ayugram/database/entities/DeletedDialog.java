package com.radolyn.ayugram.database.entities;

import androidx.room.Entity;

@Entity(primaryKeys = {"userId", "dialogId"})
public class DeletedDialog {
    public long userId;
    public long dialogId;
    public int folderId;
    public int topMessage;
    public int lastMessageDate;
    public int entityCreateDate;
}
