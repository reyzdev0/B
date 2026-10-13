package com.nabung.app;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static class Trx {
        boolean masuk;
        long jumlah;
        String catatan;
        long waktu;
    }

    private SharedPreferences prefs;
    private final List<Trx> data = new ArrayList<>();
    private long target;
    private int lastProgress = 0;

    private TextView tvSaldo, tvTarget, tvPersen, tvPesan;
    private ProgressBar progress;
    private TrxAdapter adapter;

    private final NumberFormat rp = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rp.setMaximumFractionDigits(0);
        prefs = getSharedPreferences("nabung", MODE_PRIVATE);
        load();

        tvSaldo = findViewById(R.id.tvSaldo);
        tvTarget = findViewById(R.id.tvTarget);
        tvPersen = findViewById(R.id.tvPersen);
        tvPesan = findViewById(R.id.tvPesan);
        progress = findViewById(R.id.progress);

        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrxAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btnSetor).setOnClickListener(v -> dialogTransaksi(true));
        findViewById(R.id.btnTarik).setOnClickListener(v -> dialogTransaksi(false));
        findViewById(R.id.btnTarget).setOnClickListener(v -> dialogTarget());

        refresh();
    }

    private long saldo() {
        long s = 0;
        for (Trx t : data) s += t.masuk ? t.jumlah : -t.jumlah;
        return s;
    }

    private void refresh() {
        long saldo = saldo();
        tvSaldo.setText(rp.format(saldo));
        tvTarget.setText("Target: " + rp.format(target));

        int p = target > 0 ? (int) Math.max(0, Math.min(1000, saldo * 1000 / target)) : 0;
        int pct = p / 10;

        ObjectAnimator anim = ObjectAnimator.ofInt(progress, "progress", lastProgress, p);
        anim.setDuration(600);
        anim.start();
        lastProgress = p;

        int color = pct < 30 ? 0xFFE53935 : pct < 70 ? 0xFFFB8C00 : 0xFF43A047;
        progress.setProgressTintList(ColorStateList.valueOf(color));
        tvPersen.setText(pct + "%");
        tvPersen.setTextColor(color);

        String pesan;
        if (pct >= 100) pesan = "🎉 Target tercapai!";
        else if (pct >= 70) pesan = "Sedikit lagi, semangat!";
        else if (pct >= 30) pesan = "Lumayan, terus nabung ya";
        else pesan = "Yuk mulai nabung!";
        tvPesan.setText(pesan);

        adapter.notifyDataSetChanged();
    }

    private void dialogTransaksi(boolean masuk) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText etJumlah = new EditText(this);
        etJumlah.setHint("Jumlah (Rp)");
        etJumlah.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText etCatatan = new EditText(this);
        etCatatan.setHint("Catatan (opsional)");
        box.addView(etJumlah);
        box.addView(etCatatan);

        new AlertDialog.Builder(this)
                .setTitle(masuk ? "Setor" : "Tarik")
                .setView(box)
                .setPositiveButton("Simpan", (d, w) -> {
                    long jml = parse(etJumlah.getText().toString());
                    if (jml <= 0) {
                        Toast.makeText(this, "Jumlah tidak valid", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Trx t = new Trx();
                    t.masuk = masuk;
                    t.jumlah = jml;
                    t.catatan = etCatatan.getText().toString().trim();
                    t.waktu = System.currentTimeMillis();
                    data.add(0, t);
                    save();
                    refresh();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void dialogTarget() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(target));
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(et, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Ubah Target (Rp)")
                .setView(box)
                .setPositiveButton("Simpan", (d, w) -> {
                    long t = parse(et.getText().toString());
                    if (t <= 0) {
                        Toast.makeText(this, "Target tidak valid", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    target = t;
                    save();
                    refresh();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void load() {
        target = prefs.getLong("target", 1000000);
        try {
            JSONArray a = new JSONArray(prefs.getString("data", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Trx t = new Trx();
                t.masuk = o.getBoolean("m");
                t.jumlah = o.getLong("j");
                t.catatan = o.optString("c", "");
                t.waktu = o.getLong("w");
                data.add(t);
            }
        } catch (JSONException ignored) {
        }
    }

    private void save() {
        JSONArray a = new JSONArray();
        try {
            for (Trx t : data) {
                JSONObject o = new JSONObject();
                o.put("m", t.masuk);
                o.put("j", t.jumlah);
                o.put("c", t.catatan);
                o.put("w", t.waktu);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putLong("target", target).putString("data", a.toString()).apply();
    }

    class TrxAdapter extends RecyclerView.Adapter<TrxAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView ket, waktu, jumlah;

            VH(View v) {
                super(v);
                ket = v.findViewById(R.id.tvKet);
                waktu = v.findViewById(R.id.tvWaktu);
                jumlah = v.findViewById(R.id.tvJumlah);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaksi, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Trx t = data.get(position);
            String label = t.masuk ? "Setoran" : "Penarikan";
            h.ket.setText(t.catatan.isEmpty() ? label : label + " • " + t.catatan);
            h.waktu.setText(df.format(new Date(t.waktu)));
            h.jumlah.setText((t.masuk ? "+ " : "- ") + rp.format(t.jumlah));
            h.jumlah.setTextColor(t.masuk ? 0xFF43A047 : 0xFFE53935);
            h.itemView.setOnLongClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return true;
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus transaksi?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            data.remove(pos);
                            save();
                            refresh();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
