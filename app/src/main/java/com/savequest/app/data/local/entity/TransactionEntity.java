package com.savequest.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = VaultEntity.class, parentColumns = "id",
                childColumns = "vaultId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("vaultId"), @Index("dayEpoch")})
public class TransactionEntity {
    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAW = "WITHDRAW";
    public static final String ROUNDUP = "ROUNDUP";

    @PrimaryKey(autoGenerate = true) public long id;
    public long vaultId;
    /** Positif = setor, negatif = tarik. */
    public long amount;
    public String type;
    public String note;
    public int expEarned;
    public long createdAt;
    public long dayEpoch;
}
