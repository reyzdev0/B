package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.savequest.app.data.local.entity.ShopItemEntity;

import java.util.List;

@Dao
public interface ShopItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<ShopItemEntity> list);

    @Query("SELECT * FROM shop_items WHERE enabled = 1 ORDER BY sortOrder")
    LiveData<List<ShopItemEntity>> observeEnabled();

    @Query("SELECT * FROM shop_items WHERE id = :id")
    ShopItemEntity get(String id);

    @Query("SELECT * FROM shop_items")
    List<ShopItemEntity> allSync();
}
