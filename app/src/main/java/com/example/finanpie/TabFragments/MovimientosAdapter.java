package com.example.finanpie.TabFragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanpie.Movimiento;
import com.example.finanpie.R;

import java.util.List;

public class MovimientosAdapter extends RecyclerView.Adapter<MovimientosAdapter.MovViewHolder> {
    private List<Movimiento> lista;

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
        Movimiento m = lista.get(position);
        holder.tipo.setText(holder.itemView.getContext().getString(R.string.movimiento_tipo, m.getTipo()));
        holder.monto.setText(holder.itemView.getContext().getString(R.string.movimiento_monto, m.getMonto()));
        holder.fecha.setText(holder.itemView.getContext().getString(R.string.movimiento_fecha, m.getFecha()));
    }

    @Override
    public int getItemCount() {
        return lista.size();
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
