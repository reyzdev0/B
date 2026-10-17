package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.savequest.app.data.local.entity.DayTotal;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.TxWithVault;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(TransactionEntity t);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<TransactionEntity> list);

    /** Total setoran (tanpa penarikan) pada rentang epoch day [from, to]. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE amount > 0 AND dayEpoch BETWEEN :from AND :to")
    LiveData<Long> observeDepositSum(long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE amount > 0 AND dayEpoch BETWEEN :from AND :to")
    long depositSumSync(long from, long to);

    @Query("SELECT COUNT(*) FROM transactions WHERE amount > 0")
    int depositCountSync();

    @Query("SELECT t.*, v.name AS vaultName FROM transactions t INNER JOIN vaults v ON v.id = t.vaultId ORDER BY t.createdAt DESC LIMIT :limit")
    LiveData<List<TxWithVault>> observeRecent(int limit);

    @Query("SELECT dayEpoch AS day, SUM(amount) AS total FROM transactions WHERE amount > 0 AND dayEpoch BETWEEN :from AND :to GROUP BY dayEpoch")
    LiveData<List<DayTotal>> observeDailyTotals(long from, long to);

    @Query("SELECT DISTINCT dayEpoch FROM transactions WHERE amount > 0 AND dayEpoch BETWEEN :from AND :to")
    List<Long> depositDaysSync(long from, long to);

    @Query("SELECT MIN(dayEpoch) FROM transactions WHERE amount > 0")
    Long minDepositDaySync();

    @Query("SELECT t.*, v.name AS vaultName FROM transactions t INNER JOIN vaults v ON v.id = t.vaultId WHERE t.dayEpoch BETWEEN :from AND :to ORDER BY t.createdAt ASC")
    List<TxWithVault> rangeSync(long from, long to);

    @Query("SELECT * FROM transactions")
    List<TransactionEntity> allSync();

    @Query("DELETE FROM transactions")
    void deleteAll();
}
