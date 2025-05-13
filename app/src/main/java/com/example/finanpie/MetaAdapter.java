package com.example.finanpie;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MetaAdapter extends RecyclerView.Adapter<MetaAdapter.MetaViewHolder> {

    private List<Meta> metas;
    private Context context;

    public MetaAdapter(List<Meta> metas, Context context) {
        this.metas = metas;
        this.context = context;
    }

    @NonNull
    @Override
    public MetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meta, parent, false);
        return new MetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetaViewHolder holder, int position) {
        Meta meta = metas.get(position);

        holder.tvNombreMeta.setText(meta.getNombre());

        double acumulado = meta.getAcumulado(); // 🔄 estos valores se actualizan dinámicamente
        double objetivo = meta.getObjetivo();
        int progreso = (int) ((acumulado / objetivo) * 100);

        holder.tvProgresoMeta.setText("Progreso: " + progreso + "% (" +
                acumulado + " / " + objetivo + "€)");
        holder.progressBar.setProgress(progreso);

        if (holder.cardMeta instanceof androidx.cardview.widget.CardView) {
            androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) holder.cardMeta;

            if (progreso >= 100) {
                card.setCardBackgroundColor(context.getResources().getColor(R.color.verdeSaldo));
            } else {
                card.setCardBackgroundColor(context.getResources().getColor(android.R.color.white));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Ingresar a la meta");

            final EditText input = new EditText(context);
            input.setHint("Cantidad (€)");
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            builder.setView(input);

            builder.setPositiveButton("Añadir", (dialog, which) -> {
                String ingresoStr = input.getText().toString().trim();
                if (ingresoStr.isEmpty()) {
                    Toast.makeText(context, "Ingrese una cantidad", Toast.LENGTH_SHORT).show();
                    return;
                }

                double ingreso = Double.parseDouble(ingresoStr);
                double nuevoAcumulado = acumulado + ingreso;

                if (nuevoAcumulado > objetivo) {
                    double restante = objetivo - acumulado;
                    Toast.makeText(context, "No puedes superar el objetivo. Te faltan " + restante + "€", Toast.LENGTH_LONG).show();
                    return;
                }

                meta.setAcumulado(nuevoAcumulado);
                notifyItemChanged(position);

                // 🔥 Guardar acumulado actualizado en Firebase
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String correoUsuario = user.getEmail();

                    FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("usuarios")
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                                        String correo = usuarioSnap.child("correo_electronico").getValue(String.class);

                                        if (correo != null && correo.equals(correoUsuario)) {
                                            usuarioSnap.getRef()
                                                    .child("metas")
                                                    .child(meta.getId())
                                                    .child("acumulado")
                                                    .setValue(meta.getAcumulado());
                                        }

                                        if ("admin".equals(usuarioSnap.getKey())) {
                                            usuarioSnap.getRef()
                                                    .child("metas")
                                                    .child(meta.getId())
                                                    .child("acumulado")
                                                    .setValue(meta.getAcumulado());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(context, "Error al guardar acumulado: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });


            builder.setNegativeButton("Cancelar", null);
            builder.show();
        });
    }


    @Override
    public int getItemCount() {
        return metas.size();
    }

    public static class MetaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreMeta, tvProgresoMeta;
        ProgressBar progressBar;
        View cardMeta;

        public MetaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreMeta = itemView.findViewById(R.id.tvNombreMeta);
            tvProgresoMeta = itemView.findViewById(R.id.tvProgresoMeta);
            progressBar = itemView.findViewById(R.id.progressBarMeta);
            cardMeta = itemView.findViewById(R.id.cardMeta);
        }
    }
}

