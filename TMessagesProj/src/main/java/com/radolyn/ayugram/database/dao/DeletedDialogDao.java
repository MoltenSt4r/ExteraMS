package com.radolyn.ayugram.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.radolyn.ayugram.database.entities.DeletedDialog;

import java.util.List;

@Dao
public interface DeletedDialogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeletedDialog dialog);

    @Query("SELECT * FROM DeletedDialog WHERE userId = :userId")
    List<DeletedDialog> getAll(long userId);

    @Query("SELECT EXISTS(SELECT 1 FROM DeletedDialog WHERE userId = :userId AND dialogId = :dialogId)")
    boolean exists(long userId, long dialogId);

    @Query("DELETE FROM DeletedDialog WHERE userId = :userId AND dialogId = :dialogId")
    int delete(long userId, long dialogId);

    @Query("DELETE FROM DeletedDialog WHERE userId = :userId AND dialogId IN (:dialogIds)")
    int deleteAll(long userId, List<Long> dialogIds);
}
