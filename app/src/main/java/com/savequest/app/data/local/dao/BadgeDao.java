package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.savequest.app.data.local.entity.BadgeEntity;

import java.util.List;

@Dao
public interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<BadgeEntity> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<BadgeEntity> list);

    @Update
    void update(BadgeEntity b);

    @Query("SELECT * FROM badges")
    LiveData<List<BadgeEntity>> observeAll();

    @Query("SELECT * FROM badges")
    List<BadgeEntity> allSync();

    @Query("UPDATE badges SET title = :title, description = :desc, category = :cat, tier = :tier WHERE code = :code")
    void updateMeta(String code, String title, String desc, String cat, int tier);

    @Query("SELECT * FROM badges WHERE unlockedAt > 0")
    List<BadgeEntity> unlockedSync();

    @Query("SELECT * FROM badges WHERE code = :code")
    BadgeEntity get(String code);

    @Query("DELETE FROM badges")
    void deleteAll();
}
