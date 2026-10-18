package com.example.smartdrop;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecomendacionesActivity extends BaseActivity {

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
        // Tarjeta definida en res/layout/item_tip.xml (medidas en dp, estilos del sistema de diseño).
        View card = getLayoutInflater().inflate(R.layout.item_tip, contenedorTips, false);

        // Las imágenes ilustrativas se cargan desde los recursos locales (tip_<id> si el servidor no indica otra).
        String drawable = tip.getIconoDrawable() != null ? tip.getIconoDrawable()
                : tip.getId() != null ? "tip_" + tip.getId() : null;
        int resId = drawable == null ? 0 : getResources().getIdentifier(drawable, "drawable", getPackageName());
        ImageView icono = card.findViewById(R.id.ivTipImagen);
        if (resId != 0) {
            icono.setImageResource(resId);
        } else {
            icono.setImageResource(R.drawable.ic_leaf);
        }

        TextView titulo = card.findViewById(R.id.tvTipTitulo);
        titulo.setText(tip.getTitulo());

        TextView impacto = card.findViewById(R.id.tvTipImpacto);
        impacto.setText("Impacto: " + tip.getImpacto());

        CheckBox checkbox = card.findViewById(R.id.cbTip);
        checkbox.setContentDescription("Ya practico: " + tip.getTitulo());
        return card;
    }
}
