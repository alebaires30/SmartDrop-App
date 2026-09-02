package com.example.smartdrop;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecomendacionesActivity extends AppCompatActivity {

    private ImageButton btnVolver;
    private TextView tvSaludo;
    private LinearLayout contenedorTips;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recomendaciones);

        btnVolver      = findViewById(R.id.btnVolver);
        tvSaludo       = findViewById(R.id.tvSaludo);
        contenedorTips = findViewById(R.id.contenedorTips);
        bottomNav      = findViewById(R.id.bottomNavRecom);

        btnVolver.setOnClickListener(v -> finish());
        bottomNav.setSelectedItemId(R.id.nav_recomendaciones);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_consumo) {
                startActivity(new Intent(this, ConsumoActivity.class));
                finish();
            } else if (id == R.id.nav_retroalimentacion) {
                startActivity(new Intent(this, RetroalimentacionActivity.class));
                finish();
            }
            return true;
        });

        cargarRecomendaciones();
    }

    private void cargarRecomendaciones() {
        ApiService api = ApiClient.getClientAutenticado(this).create(ApiService.class);
        api.obtenerRecomendaciones().enqueue(new Callback<RecomendacionesResponse>() {
            @Override
            public void onResponse(Call<RecomendacionesResponse> call, Response<RecomendacionesResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                RecomendacionesResponse r = response.body();

                tvSaludo.setText(r.getSaludo());
                contenedorTips.removeAllViews();

                for (RecomendacionTip tip : r.getTips()) {
                    contenedorTips.addView(construirTarjetaTip(tip));
                }
            }

            @Override
            public void onFailure(Call<RecomendacionesResponse> call, Throwable t) { }
        });
    }

    private View construirTarjetaTip(RecomendacionTip tip) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsCard.bottomMargin = 24;
        card.setLayoutParams(paramsCard);
        card.setRadius(28f);
        card.setCardElevation(6f);

        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(32, 24, 32, 24);
        fila.setGravity(Gravity.CENTER_VERTICAL);

        // ⚠️ Aquí se referencia la imagen real que agregarás tú a res/drawable/
        int resId = getResources().getIdentifier(tip.getIconoDrawable(), "drawable", getPackageName());
        if (resId != 0) {
            ImageView icono = new ImageView(this);
            LinearLayout.LayoutParams paramsIcono = new LinearLayout.LayoutParams(96, 96);
            paramsIcono.topMargin = 24;
            icono.setLayoutParams(paramsIcono);
            icono.setImageResource(resId);
            fila.addView(icono);
        }

        LinearLayout columnaTexto = new LinearLayout(this);
        columnaTexto.setOrientation(LinearLayout.VERTICAL);
        columnaTexto.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView titulo = new TextView(this);
        titulo.setText("TIP: " + tip.getTitulo());
        titulo.setTextSize(14);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.parseColor("#3D3D3D"));

        TextView impacto = new TextView(this);
        impacto.setText("IMPACTO: " + tip.getImpacto());
        impacto.setTextSize(12);
        impacto.setTextColor(Color.parseColor("#623398"));

        columnaTexto.addView(titulo);
        columnaTexto.addView(impacto);

        CheckBox checkbox = new CheckBox(this);

        fila.addView(columnaTexto);
        fila.addView(checkbox);
        card.addView(fila);
        return card;
    }
}