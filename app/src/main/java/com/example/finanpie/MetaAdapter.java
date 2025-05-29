package com.example.finanpie;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetaAdapter extends RecyclerView.Adapter<MetaAdapter.MetaViewHolder> {

    private final List<Meta> metas;
    private final Context context;
    private final Set<String> animacionesAplicadas = new HashSet<>();


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
        double acumulado = meta.getAcumulado();
        double objetivo = meta.getObjetivo();
        int progreso = (int) ((acumulado / objetivo) * 100);

        holder.tvNombreMeta.setText(meta.getNombre());
        holder.tvProgresoMeta.setText(context.getString(R.string.progreso_meta, progreso, acumulado, objetivo));
        holder.progressBar.setProgress(progreso);

        if (holder.cardMeta instanceof CardView) {
            ((CardView) holder.cardMeta).setCardBackgroundColor(context.getResources().getColor(
                    progreso >= 100 ? R.color.verde_esmeralda : android.R.color.white));
        }

        holder.itemView.setOnClickListener(v -> mostrarDialogoIngresarCantidad(meta, position));

        if (!animacionesAplicadas.contains(meta.getId())) {
            holder.itemView.animate()
                    .translationX(10)
                    .setDuration(100)
                    .withEndAction(() -> holder.itemView.animate()
                            .translationX(-10)
                            .setDuration(100)
                            .withEndAction(() -> holder.itemView.animate()
                                    .translationX(0)
                                    .setDuration(100)
                                    .start())
                            .start())
                    .start();

            animacionesAplicadas.add(meta.getId());
        }


    }
    public void reiniciarAnimaciones() {
        animacionesAplicadas.clear();
    }

    @Override
    public int getItemCount() {
        return metas.size();
    }

    private void mostrarDialogoIngresarCantidad(Meta meta, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_titulo_ingresar_meta);

        final EditText input = new EditText(context);
        input.setHint(R.string.hint_cantidad_meta);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton(R.string.btn_aniadir, (dialog, which) -> {
            String ingresoStr = input.getText().toString().trim();
            if (ingresoStr.isEmpty()) {
                Toast.makeText(context, R.string.toast_ingrese_cantidad, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double ingreso = Double.parseDouble(ingresoStr);
                double nuevoAcumulado = meta.getAcumulado() + ingreso;

                if (nuevoAcumulado > meta.getObjetivo()) {
                    double restante = meta.getObjetivo() - meta.getAcumulado();
                    Toast.makeText(context,
                            context.getString(R.string.toast_no_superar_objetivo, restante),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                meta.setAcumulado(nuevoAcumulado);
                notifyItemChanged(position);
                guardarAcumuladoEnFirebase(meta);

            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.formato_invalido, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.btn_cancelar, null);
        builder.show();
    }

    private void guardarAcumuladoEnFirebase(Meta meta) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();
        DatabaseReference refUsuarios = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                    String correo = usuarioSnap.child("correo_electronico").getValue(String.class);
                    String key = usuarioSnap.getKey();

                    if ((correo != null && correo.equals(correoUsuario)) || "admin".equals(key)) {
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
                Toast.makeText(context,
                        context.getString(R.string.toast_error_guardar_acumulado, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
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
