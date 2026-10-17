package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.savequest.app.data.local.entity.PlayerEntity;

@Dao
public interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(PlayerEntity p);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replace(PlayerEntity p);

    @Update
    void update(PlayerEntity p);

    @Query("SELECT * FROM player WHERE id = 1")
    LiveData<PlayerEntity> observe();

    @Query("SELECT * FROM player WHERE id = 1")
    PlayerEntity get();

    @Query("DELETE FROM player")
    void deleteAll();
}
