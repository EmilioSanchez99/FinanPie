package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.finanpie.MainActivity;
import com.example.finanpie.Movimiento;
import com.example.finanpie.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorialFragment extends Fragment {
    private RecyclerView recyclerView;
    private MovimientosAdapter adapter;
    private List<Movimiento> listaOriginal;
    private List<Movimiento> listaFiltrada;
    private DatabaseReference usuariosRef;
    private FirebaseAuth mAuth;
    private String fechaFiltrada = "";
    private ImageButton btnCalendario;

    private Button btnOrdenar;
    private boolean ordenAscendente = false;
    private Button btnQuitarFiltro, btnEliminarTodos;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        recyclerView = view.findViewById(R.id.recyclerMovimientos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnOrdenar = view.findViewById(R.id.btnOrdenar);

        listaOriginal = new ArrayList<>();
        listaFiltrada = new ArrayList<>();
        adapter = new MovimientosAdapter(listaFiltrada);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        cargarMovimientos();

        btnEliminarTodos = view.findViewById(R.id.btnEliminarTodos);
        btnEliminarTodos.setOnClickListener(v -> confirmarEliminacionTodos());


        btnCalendario = view.findViewById(R.id.btnCalendario);
        btnCalendario.setOnClickListener(v -> mostrarSelectorFecha());

        btnOrdenar.setOnClickListener(v -> {
            ordenAscendente = !ordenAscendente;
            btnOrdenar.setText("Orden: " + (ordenAscendente ? "Ascendente" : "Descendente"));
            filtrarYOrdenar();
        });


        btnQuitarFiltro = view.findViewById(R.id.btnQuitarFiltro);
        btnQuitarFiltro.setOnClickListener(v -> {
            fechaFiltrada = "";
            filtrarYOrdenar();
        });


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Movimiento movimiento = listaFiltrada.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar movimiento")
                        .setMessage("¿Estás seguro de que quieres eliminar este movimiento?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            eliminarMovimientoDeFirebase(movimiento);
                            listaFiltrada.remove(position);
                            adapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // restaurar si cancela
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }

        }).attachToRecyclerView(recyclerView);


        Button btnExportar = view.findViewById(R.id.btnExportarCSV);
        btnExportar.setOnClickListener(v -> exportarMovimientosACSV());



        return view;
    }

    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    fechaFiltrada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    filtrarYOrdenar();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void eliminarMovimientoDeFirebase(Movimiento movimiento) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || movimiento.getKey() == null) return;

        String correoUsuario = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) {
                        DatabaseReference userRef = ds.getRef();

                        // ✅ Eliminar el movimiento
                        userRef.child("movimientos").child(movimiento.getKey()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    // ✅ Actualizar saldos
                                    Double saldo = ds.child("saldo").getValue(Double.class);
                                    Double saldoGastado = ds.child("saldo_gastado").getValue(Double.class);

                                    if (saldo == null) saldo = 0.0;
                                    if (saldoGastado == null) saldoGastado = 0.0;

                                    double nuevoSaldo = movimiento.getTipo().equals("ingreso")
                                            ? saldo - movimiento.getMonto()
                                            : saldo + movimiento.getMonto();

                                    double nuevoGastado = movimiento.getTipo().equals("retirar")
                                            ? saldoGastado - movimiento.getMonto()
                                            : saldoGastado;

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("saldo", nuevoSaldo);
                                    updates.put("saldo_gastado", nuevoGastado);

                                    userRef.updateChildren(updates).addOnCompleteListener(task -> {
                                        Toast.makeText(getContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show();

                                        // 🔁 Refrescar gráfico si el PrincipalFragment está cargado
                                        if (getActivity() instanceof MainActivity) {
                                            PrincipalFragment principalFragment = ((MainActivity) getActivity()).getPrincipalFragment();
                                            if (principalFragment != null && principalFragment.isAdded()) {
                                                principalFragment.refrescarGrafico();
                                            }
                                        }
                                    });
                                });

                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarEliminacionTodos() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar todos los movimientos")
                .setMessage("¿Estás seguro de que quieres borrar todos los movimientos?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarTodosLosMovimientos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTodosLosMovimientos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) {
                        DatabaseReference userRef = ds.getRef();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("movimientos", null);        // 🔥 borra todos los movimientos
                        updates.put("saldo", 0.0);               // 💰 saldo = 0
                        updates.put("saldo_gastado", 0.0);       // 💸 saldo_gastado = 0

                        userRef.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Todo eliminado y saldos reiniciados", Toast.LENGTH_SHORT).show();
                                    cargarMovimientos(); // 🔁 volver a cargar desde Firebase
                                });


                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al eliminar movimientos", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void cargarMovimientos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaOriginal.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) {
                        DataSnapshot movimientosSnap = ds.child("movimientos");
                        for (DataSnapshot mov : movimientosSnap.getChildren()) {
                            Movimiento movimiento = mov.getValue(Movimiento.class);
                            if (movimiento != null) {
                                movimiento.setKey(mov.getKey());
                                listaOriginal.add(movimiento);

                            }
                        }
                        filtrarYOrdenar();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar movimientos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarYOrdenar() {
        listaFiltrada.clear();

        for (Movimiento m : listaOriginal) {
            // Aquí se filtra por fecha
            if (fechaFiltrada.isEmpty() || m.getFecha().equals(fechaFiltrada)) {
                listaFiltrada.add(m);
            }
        }

        // Aquí se ordena por fecha
        listaFiltrada.sort((m1, m2) -> {
            int comp = m1.getFecha().compareTo(m2.getFecha());
            return ordenAscendente ? comp : -comp;
        });

        adapter.notifyDataSetChanged();
    }


    @Override
    public void onResume() {
        super.onResume();
        cargarMovimientos(); // 🔁 vuelve a cargar Firebase cada vez que el usuario entra
    }
    private void exportarMovimientosACSV() {
        if (listaOriginal.isEmpty()) {
            Toast.makeText(getContext(), "No hay movimientos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder data = new StringBuilder();
        data.append("Tipo,Monto,Fecha\n");

        for (Movimiento m : listaOriginal) {
            data.append(m.getTipo()).append(",")
                    .append(m.getMonto()).append(",")
                    .append(m.getFecha()).append("\n");
        }

        try {
            String fileName = "movimientos_finanpie.csv";
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!documentsDir.exists()) documentsDir.mkdirs();

            File file = new File(documentsDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.toString().getBytes());
            fos.close();

            Toast.makeText(getContext(), "Exportado a Documents:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al exportar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }




}
