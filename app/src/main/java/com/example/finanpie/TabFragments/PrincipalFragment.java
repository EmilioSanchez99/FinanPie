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

        mAuth = FirebaseAuth.getInstance(); // 👈 inicializar aquí
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
        cargarNombreYSaldo(); // ya existente
    }
    private void mostrarDialogoMovimiento(String tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(tipo.equals("ingreso") ? "Ingresar saldo" : "Retirar saldo");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Cantidad (€)");

        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String valor = input.getText().toString().trim();
            if (valor.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese una cantidad válida", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double monto = Double.parseDouble(valor);
                if (monto <= 0) {
                    Toast.makeText(requireContext(), "La cantidad debe ser mayor que 0", Toast.LENGTH_SHORT).show();
                } else {
                    agregarMovimiento(tipo, monto);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Formato inválido", Toast.LENGTH_SHORT).show();
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
                            txtNombreUsuario.setText("Hola, " + nombre);
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
                Toast.makeText(getContext(), "Error al cargar el perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void mostrarGraficoSaldo(double saldo, double saldoGastado) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) saldo, "Saldo Disponible"));
        entries.add(new PieEntry((float) saldoGastado, "Gastado"));

        int verde = ContextCompat.getColor(requireContext(), R.color.verdeSaldo);
        int rojoClaro = ContextCompat.getColor(requireContext(), R.color.rojoGastado);
        int fondo = ContextCompat.getColor(requireContext(), R.color.background_primary);


        PieDataSet dataSet = new PieDataSet(entries, "Resumen de saldo");
        dataSet.setColors(verde, rojoClaro);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setHoleColor(fondo);
        pieChart.invalidate(); // refrescar gráfico
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
                            Toast.makeText(getContext(), "Saldo insuficiente. Tienes disponible: " + saldoActual + "€", Toast.LENGTH_LONG).show();
                            return;
                        }

                        double nuevoSaldo = tipo.equals("ingreso") ? saldoActual + monto : saldoActual - monto;

                        // Actualizar saldo
                        ds.getRef().child("saldo").setValue(nuevoSaldo);

                        // 👇 ACTUALIZAR SALDO GASTADO
                        if (tipo.equals("retirar")) {
                            Double saldoGastadoActual = ds.child("saldo_gastado").getValue(Double.class);
                            if (saldoGastadoActual == null) saldoGastadoActual = 0.0;
                            double nuevoGastado = saldoGastadoActual + monto;
                            ds.getRef().child("saldo_gastado").setValue(nuevoGastado);
                        }

                        // Crear movimiento
                        long totalMovs = ds.child("movimientos").getChildrenCount();
                        String nuevoId = "mov" + (totalMovs + 1);

                        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        Map<String, Object> nuevoMovimiento = new HashMap<>();
                        nuevoMovimiento.put("tipo", tipo);
                        nuevoMovimiento.put("monto", monto);
                        nuevoMovimiento.put("fecha", fecha);

                        ds.getRef().child("movimientos").child(nuevoId).setValue(nuevoMovimiento)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Movimiento registrado", Toast.LENGTH_SHORT).show();
                                    cargarNombreYSaldo(); // refrescar vista
                                });

                        return;
                    }
                }

                Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al registrar el movimiento", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
