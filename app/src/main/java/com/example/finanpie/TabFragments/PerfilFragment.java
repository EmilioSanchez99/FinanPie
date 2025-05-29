package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.finanpie.R;


public class PerfilFragment extends Fragment {



    public PerfilFragment() {


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        CardView recomendarCard = view.findViewById(R.id.recomendarAmigos);
        CardView premiumCard = view.findViewById(R.id.premium);

        premiumCard.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Hazte Premium");
            builder.setMessage("Desbloquea funciones exclusivas como estadísticas avanzadas, sin anuncios y más.");
            builder.setPositiveButton("Comprar", (dialog, which) -> {
                // Aquí puedes lanzar la lógica de compra o mostrar un Toast
                Toast.makeText(getContext(), "Función aún no disponible", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancelar", null);
            builder.show();
        });

        recomendarCard.setOnClickListener(v -> {
            try {

                Uri smsUri = Uri.parse("smsto:");
                Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
                intent.putExtra("sms_body", "¡Descubre FinanPie! Tu nueva app de finanzas personales 🤑📱");

                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No se pudo abrir el panel de mensajes", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}