package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.savequest.app.data.local.entity.VaultEntity;

import java.util.List;

@Dao
public interface VaultDao {
    @Insert
    long insert(VaultEntity v);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<VaultEntity> list);

    @Delete
    void delete(VaultEntity v);

    @Update
    void update(VaultEntity v);

    @Query("SELECT * FROM vaults ORDER BY createdAt DESC")
    LiveData<List<VaultEntity>> observeAll();

    @Query("SELECT * FROM vaults")
    List<VaultEntity> allSync();

    @Query("SELECT * FROM vaults WHERE id = :id")
    VaultEntity getById(long id);

    @Query("UPDATE vaults SET savedAmount = savedAmount + :delta WHERE id = :id")
    void addSaved(long id, long delta);

    @Query("SELECT COALESCE(SUM(savedAmount), 0) FROM vaults")
    LiveData<Long> observeTotal();

    @Query("SELECT COALESCE(SUM(savedAmount), 0) FROM vaults")
    long totalSync();

    @Query("SELECT COUNT(*) FROM vaults WHERE targetAmount > 0 AND savedAmount >= targetAmount")
    int completedCount();

    @Query("SELECT COUNT(*) FROM vaults WHERE type = 'BOSS' AND defeatedAt > 0")
    int bossDefeatedCount();

    @Query("DELETE FROM vaults")
    void deleteAll();
}
