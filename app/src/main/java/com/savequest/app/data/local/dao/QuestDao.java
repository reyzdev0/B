package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.savequest.app.data.local.entity.QuestEntity;

import java.util.List;

@Dao
public interface QuestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(QuestEntity q);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<QuestEntity> list);

    @Update
    void update(QuestEntity q);

    @Query("SELECT * FROM quests WHERE id = :id")
    QuestEntity getById(long id);

    @Query("SELECT * FROM quests WHERE periodStart <= :day AND periodEnd >= :day ORDER BY rewardExp")
    LiveData<List<QuestEntity>> observeActive(long day);

    @Query("SELECT * FROM quests WHERE periodStart <= :day AND periodEnd >= :day")
    List<QuestEntity> activeSync(long day);

    @Query("SELECT * FROM quests")
    List<QuestEntity> allSync();

    @Query("DELETE FROM quests")
    void deleteAll();
}
