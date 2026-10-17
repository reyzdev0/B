package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.savequest.app.data.local.entity.OwnedItemEntity;

import java.util.List;

@Dao
public interface OwnedItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(OwnedItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<OwnedItemEntity> list);

    @Query("SELECT * FROM owned_items")
    LiveData<List<OwnedItemEntity>> observeAll();

    @Query("SELECT * FROM owned_items")
    List<OwnedItemEntity> allSync();

    @Query("SELECT * FROM owned_items WHERE itemId = :id")
    OwnedItemEntity get(String id);

    @Query("DELETE FROM owned_items")
    void deleteAll();
}
