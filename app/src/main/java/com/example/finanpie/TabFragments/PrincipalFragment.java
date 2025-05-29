package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finanpie.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PrincipalFragment extends Fragment {

    private TextView txtNombreUsuario;
    private FirebaseAuth mAuth;
    private DatabaseReference usuariosRef;
    private PieChart pieChart;
    private Button btnIngresar, btnRetirar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_principal, container, false);

        txtNombreUsuario = view.findViewById(R.id.txtNombreUsuario);
        pieChart = view.findViewById(R.id.pieChart);
        btnIngresar = view.findViewById(R.id.btnIngresar);
        btnRetirar = view.findViewById(R.id.btnRetirar);

        mAuth = FirebaseAuth.getInstance();
        usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app").getReference("usuarios");

        cargarNombreYSaldo();

        btnIngresar.setOnClickListener(v -> mostrarDialogoMovimiento("ingreso"));
        btnRetirar.setOnClickListener(v -> mostrarDialogoMovimiento("retirar"));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarNombreYSaldo();
    }

    public void refrescarGrafico() {
        cargarNombreYSaldo();
    }

    private void mostrarDialogoMovimiento(String tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(tipo.equals("ingreso") ? getString(R.string.ingresar_saldo) : getString(R.string.retirar_saldo));

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(getString(R.string.hint_cantidad));

        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String valor = input.getText().toString().trim();
            if (valor.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.cantidad_invalida), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double monto = Double.parseDouble(valor);
                if (monto <= 0) {
                    Toast.makeText(requireContext(), getString(R.string.cantidad_menor_cero), Toast.LENGTH_SHORT).show();
                } else {
                    agregarMovimiento(tipo, monto);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), getString(R.string.formato_invalido), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void cargarNombreYSaldo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String emailUsuario = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(emailUsuario)) {
                        String nombre = ds.child("nombre").getValue(String.class);
                        Double saldo = ds.child("saldo").getValue(Double.class);
                        Double saldoGastado = ds.child("saldo_gastado").getValue(Double.class);

                        if (nombre != null) {
                            nombre = nombre.replace("\"", "");
                            txtNombreUsuario.setText(getString(R.string.saludo_usuario, nombre));
                        }

                        if (saldo == null) saldo = 0.0;
                        if (saldoGastado == null) saldoGastado = 0.0;

                        mostrarGraficoSaldo(saldo, saldoGastado);
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.error_cargar_perfil), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarGraficoSaldo(double saldo, double saldoGastado) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) saldo, getString(R.string.grafico_saldo_disponible)));
        entries.add(new PieEntry((float) saldoGastado, getString(R.string.grafico_saldo_gastado)));
        pieChart.getDescription().setEnabled(false);

        int verde = ContextCompat.getColor(requireContext(), R.color.verde_esmeralda);
        int rojoClaro = ContextCompat.getColor(requireContext(), R.color.rojo_suave);
        int fondo = ContextCompat.getColor(requireContext(), R.color.gris_claro);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(verde, rojoClaro);
        dataSet.setValueTextColor(R.color.azul_marino);
        dataSet.setValueTextSize(20f);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setHoleColor(fondo);
        pieChart.invalidate();
    }

    private void agregarMovimiento(String tipo, double monto) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) {

                        Double saldoActual = ds.child("saldo").getValue(Double.class);
                        if (saldoActual == null) saldoActual = 0.0;

                        if (tipo.equals("retirar") && monto > saldoActual) {
                            Toast.makeText(getContext(), getString(R.string.saldo_insuficiente, saldoActual), Toast.LENGTH_LONG).show();
                            return;
                        }

                        double nuevoSaldo = tipo.equals("ingreso") ? saldoActual + monto : saldoActual - monto;

                        ds.getRef().child("saldo").setValue(nuevoSaldo);

                        if (tipo.equals("retirar")) {
                            Double saldoGastadoActual = ds.child("saldo_gastado").getValue(Double.class);
                            if (saldoGastadoActual == null) saldoGastadoActual = 0.0;
                            double nuevoGastado = saldoGastadoActual + monto;
                            ds.getRef().child("saldo_gastado").setValue(nuevoGastado);
                        }

                        long totalMovs = ds.child("movimientos").getChildrenCount();
                        String nuevoId = "mov" + (totalMovs + 1);

                        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        Map<String, Object> nuevoMovimiento = new HashMap<>();
                        nuevoMovimiento.put("tipo", tipo);
                        nuevoMovimiento.put("monto", monto);
                        nuevoMovimiento.put("fecha", fecha);

                        ds.getRef().child("movimientos").child(nuevoId).setValue(nuevoMovimiento)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), getString(R.string.movimiento_registrado), Toast.LENGTH_SHORT).show();
                                    cargarNombreYSaldo();
                                });

                        return;
                    }
                }

                Toast.makeText(getContext(), getString(R.string.usuario_no_encontrado), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.error_registrar_movimiento), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
