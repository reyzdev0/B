package com.savequest.app.data.local.entity;

import androidx.room.Embedded;

/** Hasil join transaksi + nama vault untuk daftar aktivitas. */
public class TxWithVault {
    @Embedded public TransactionEntity tx;
    public String vaultName;
}
