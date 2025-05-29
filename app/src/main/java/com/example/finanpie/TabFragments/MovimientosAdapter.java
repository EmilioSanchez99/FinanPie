package com.example.finanpie.TabFragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanpie.Movimiento;
import com.example.finanpie.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovimientosAdapter extends RecyclerView.Adapter<MovimientosAdapter.MovViewHolder> {

    private final List<Movimiento> lista;
    private final Set<String> animacionesAplicadas = new HashSet<>();

    public MovimientosAdapter(List<Movimiento> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public MovViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movimiento, parent, false);
        return new MovViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MovViewHolder holder, int position) {
        Movimiento movimiento = lista.get(position);

        holder.tipo.setText(holder.itemView.getContext().getString(R.string.movimiento_tipo, movimiento.getTipo()));
        holder.monto.setText(holder.itemView.getContext().getString(R.string.movimiento_monto, movimiento.getMonto()));
        holder.fecha.setText(holder.itemView.getContext().getString(R.string.movimiento_fecha, movimiento.getFecha()));

        if (movimiento.getKey() != null && !animacionesAplicadas.contains(movimiento.getKey())) {
            holder.itemView.animate()
                    .translationX(25)
                    .setDuration(150)
                    .withEndAction(() -> holder.itemView.animate()
                            .translationX(-25)
                            .setDuration(150)
                            .withEndAction(() -> holder.itemView.animate()
                                    .translationX(15)
                                    .setDuration(120)
                                    .withEndAction(() -> holder.itemView.animate()
                                            .translationX(0)
                                            .setDuration(100)
                                            .start())
                                    .start())
                            .start())
                    .start();

            animacionesAplicadas.add(movimiento.getKey());
        }

    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void reiniciarAnimaciones() {
        animacionesAplicadas.clear();
    }

    static class MovViewHolder extends RecyclerView.ViewHolder {
        TextView tipo, monto, fecha;

        public MovViewHolder(View itemView) {
            super(itemView);
            tipo = itemView.findViewById(R.id.txtTipo);
            monto = itemView.findViewById(R.id.txtMonto);
            fecha = itemView.findViewById(R.id.txtFecha);
        }
    }
}
