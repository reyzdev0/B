# SaveQuest v2.2 - Economy, Inventory & Maintenance Update

Android native (Java + XML), MVVM, Room. Build dan rilis otomatis lewat GitHub Actions.

## 5 fitur baru
| # | Fitur | Lokasi |
|---|-------|--------|
| 1 | Update otomatis tanpa uninstall (PackageInstaller) | `update/`, `ui/settings/UpdateFlow`, `app/keystore/`, `.github/workflows/build.yml` |
| 2 | Inventory (Ransel) dengan grid, filter, quantity, tombol Gunakan | `ui/inventory/`, `ui/views/ItemSlotView` |
| 3 | Toko item consumable (Perban, Ramuan, Perisai Streak, Booster, dll) | `ui/shop/`, `domain/ItemCatalog` |
| 4 | 57 badge, tier Bronze sampai Diamond + Rahasia, 17 jenis trigger | `domain/BadgeCatalog`, `domain/BadgeEngine` |
| 5 | Item Effect Manager (heal, shield, booster, EXP, koin, spin, peti) | `domain/ItemEffectManager`, `SavingsRepository.useItem` |

## Cara kerja update tanpa uninstall
1. Setiap push ke `main`, GitHub Actions membuild APK dan membuat **Release** (`v2.2.<nomor build>`).
   `versionCode = 1000 + nomor build`, tertulis juga di isi rilis.
2. Di app: Pengaturan > Pembaruan Aplikasi > Sumber update, isi `pemilik/repo` (repo harus publik).
3. App membandingkan versionCode, mengunduh APK, memverifikasi (package sama + versi lebih tinggi),
   lalu memasang lewat `PackageInstaller`. Android menimpa app lama dan data internal tetap utuh.
4. Syarat Android: tanda tangan APK harus identik. Karena itu project memakai kunci tetap
   `app/keystore/savequest.p12` (alias/password: savequest) untuk semua build.
5. Sebelum memasang, app menyimpan snapshot JSON (3 terakhir). Tombol "Pulihkan snapshot" ada di pengaturan.

### Pemasangan pertama v2.2 (sekali saja)
Build lama memakai kunci debug acak, jadi v2.2 tidak bisa menimpa versi lama:
1. Di app lama: Pengaturan > Backup & Restore > Export JSON.
2. Uninstall app lama, install v2.2, lalu Import JSON.
Foto avatar dari galeri tidak ikut backup. Setelah itu semua update berikutnya bisa menimpa langsung.

## Item
| Item | Efek | Harga |
|------|------|-------|
| Perban / Ramuan HP / Elixir Kehidupan | HP +15 / +40 / penuh | 25 / 60 / 140 |
| Perisai Streak | otomatis menyelamatkan streak jika terlewat 1 hari | 80 |
| Gulungan Pelindung | otomatis menyelamatkan streak sampai 3 hari | 200 |
| Ramuan EXP | EXP x1.5 untuk 3 setoran | 70 |
| Elixir EXP | EXP x2 selama 24 jam | 160 |
| Gulungan Ilmu | +100 EXP | 120 |
| Tiket Spin | satu spin tambahan | 90 |
| Kantong Koin | 30-90 koin acak | 60 |
| Peti Kosmetik | 1 kosmetik acak yang belum dimiliki | 220 |

Sumber item selain beli: hadiah roda (Perban, Ramuan EXP, Perisai), klaim quest, dan mengalahkan boss.
Booster item dan buff roda tidak ditumpuk: yang dipakai adalah yang terbesar.

## Database
Room versi 4, migrasi 1 -> 2 -> 3 -> 4 tanpa fallbackToDestructiveMigration.
Tabel baru: `inventory`, `shop_items`. Penghitung badge diisi ulang dari riwayat transaksi saat migrasi.
