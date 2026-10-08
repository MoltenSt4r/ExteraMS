/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.dao.EditedMessageDao;
import com.radolyn.ayugram.database.dao.LastSeenDao;
import com.radolyn.ayugram.database.dao.SpyDao;
import com.radolyn.ayugram.database.dao.DeletedDialogDao;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageReaction;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.database.entities.LastSeenEntity;
import com.radolyn.ayugram.database.entities.SpyMessageRead;
import com.radolyn.ayugram.database.entities.SpyMessageContentsRead;
import com.radolyn.ayugram.database.entities.DeletedDialog;

@Database(entities = {
        EditedMessage.class,
        DeletedMessage.class,
        DeletedMessageReaction.class,
        LastSeenEntity.class,
        SpyMessageRead.class,
        SpyMessageContentsRead.class,
        DeletedDialog.class
}, version = AyuDatabase.VERSION)
public abstract class AyuDatabase extends RoomDatabase {
    public static final int MIN_SUPPORTED_VERSION = 21;
    public static final int VERSION = 27;

    public abstract EditedMessageDao editedMessageDao();

    public abstract DeletedMessageDao deletedMessageDao();

    public abstract LastSeenDao lastSeenDao();

    public abstract SpyDao spyDao();

    public abstract DeletedDialogDao deletedDialogDao();
}
