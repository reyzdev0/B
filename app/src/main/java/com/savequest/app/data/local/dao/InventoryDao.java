package com.savequest.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.savequest.app.data.local.entity.InventoryEntity;

import java.util.List;

@Dao
public interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(InventoryEntity e);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllReplace(List<InventoryEntity> list);

    @Query("UPDATE inventory SET quantity = quantity + :qty WHERE itemId = :id")
    void increase(String id, int qty);

    /** Kurangi 1 jika masih ada. Mengembalikan jumlah baris yang berubah (0 = stok habis). */
    @Query("UPDATE inventory SET quantity = quantity - 1 WHERE itemId = :id AND quantity > 0")
    int consumeOne(String id);

    @Query("DELETE FROM inventory WHERE quantity <= 0")
    void deleteEmpty();

    @Query("SELECT * FROM inventory WHERE quantity > 0 ORDER BY acquiredAt DESC")
    LiveData<List<InventoryEntity>> observeAll();

    @Query("SELECT * FROM inventory WHERE quantity > 0")
    List<InventoryEntity> allSync();

    @Query("SELECT COALESCE(quantity, 0) FROM inventory WHERE itemId = :id")
    int quantityOf(String id);

    @Query("DELETE FROM inventory")
    void deleteAll();
}
