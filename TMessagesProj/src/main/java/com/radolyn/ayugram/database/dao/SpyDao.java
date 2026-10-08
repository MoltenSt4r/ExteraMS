package com.radolyn.ayugram.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radolyn.ayugram.database.entities.SpyMessageContentsRead;
import com.radolyn.ayugram.database.entities.SpyMessageRead;

import java.util.List;

@Dao
public interface SpyDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRead(List<SpyMessageRead> reads);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertContentsRead(List<SpyMessageContentsRead> reads);

    @Query("SELECT date FROM SpyMessageRead WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId")
    Integer getReadDate(long userId, long dialogId, int messageId);

    @Query("SELECT date FROM SpyMessageContentsRead WHERE userId = :userId AND dialogId = :dialogId AND messageId = :messageId")
    Integer getContentsReadDate(long userId, long dialogId, int messageId);
}
