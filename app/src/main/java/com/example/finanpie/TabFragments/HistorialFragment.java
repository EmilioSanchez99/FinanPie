package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanpie.MainActivity;
import com.example.finanpie.Movimiento;
import com.example.finanpie.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovimientosAdapter adapter;
    private List<Movimiento> listaOriginal = new ArrayList<>();
    private List<Movimiento> listaFiltrada = new ArrayList<>();
    private DatabaseReference usuariosRef;
    private FirebaseAuth mAuth;
    private String fechaFiltrada = "";
    private boolean ordenAscendente = false;

    private ImageButton btnCalendario, btnOrdenar, btnQuitarFiltro;
    private Button btnEliminarTodos;

    private TextView txtOrdenarEstado;

    private final Set<String> animacionesAplicadas = new HashSet<>();



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        initFirebase();
        initViews(view);
        setupRecyclerView();
        setupButtons();
        setupSwipeToDelete();

        cargarMovimientos();
        return view;
    }



    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerMovimientos);
        btnCalendario = view.findViewById(R.id.btnCalendario);
        btnOrdenar = view.findViewById(R.id.btnOrdenar);
        btnQuitarFiltro = view.findViewById(R.id.btnQuitarFiltro);
        btnEliminarTodos = view.findViewById(R.id.btnEliminarTodos);
        txtOrdenarEstado = view.findViewById(R.id.txtOrden);
        Button btnExportar = view.findViewById(R.id.btnExportarCSV);
        btnExportar.setOnClickListener(v -> exportarMovimientosACSV());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MovimientosAdapter(listaFiltrada);
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        btnEliminarTodos.setOnClickListener(v -> confirmarEliminacionTodos());
        btnCalendario.setOnClickListener(v -> mostrarSelectorFecha());
        btnOrdenar.setOnClickListener(v -> toggleOrden());
        btnQuitarFiltro.setOnClickListener(v -> {
            fechaFiltrada = "";
            filtrarYOrdenar();
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Movimiento movimiento = listaFiltrada.get(position);
                mostrarDialogoEliminar(movimiento, position);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void toggleOrden() {
        ordenAscendente = !ordenAscendente;
        txtOrdenarEstado.setText(getString(ordenAscendente ? R.string.orden_ascendente : R.string.orden_descendente));
        filtrarYOrdenar();
    }

    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            fechaFiltrada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            filtrarYOrdenar();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarDialogoEliminar(Movimiento movimiento, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.titulo_eliminar_movimiento)
                .setMessage(R.string.mensaje_eliminar_movimiento)
                .setPositiveButton(R.string.boton_si, (dialog, which) -> {
                    eliminarMovimientoDeFirebase(movimiento);
                    listaFiltrada.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton(R.string.boton_cancelar, (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void confirmarEliminacionTodos() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.titulo_eliminar_todo)
                .setMessage(R.string.mensaje_eliminar_todo)
                .setPositiveButton(R.string.boton_si, (dialog, which) -> eliminarTodosLosMovimientos())
                .setNegativeButton(R.string.boton_cancelar, null)
                .show();
    }

    private void filtrarYOrdenar() {
        listaFiltrada.clear();
        for (Movimiento m : listaOriginal) {
            if (fechaFiltrada.isEmpty() || m.getFecha().equals(fechaFiltrada)) {
                listaFiltrada.add(m);
            }
        }
        listaFiltrada.sort((m1, m2) -> ordenAscendente ? m1.getFecha().compareTo(m2.getFecha()) : m2.getFecha().compareTo(m1.getFecha()));
        adapter.notifyDataSetChanged();
    }

    private void exportarMovimientosACSV() {
        if (listaOriginal.isEmpty()) {
            Toast.makeText(getContext(), R.string.sin_movimientos, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder data = new StringBuilder("Tipo,Monto,Fecha\n");
        for (Movimiento m : listaOriginal) {
            data.append(m.getTipo()).append(",").append(m.getMonto()).append(",").append(m.getFecha()).append("\n");
        }

        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "movimientos_finanpie.csv");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.toString().getBytes());
            fos.close();
            Toast.makeText(getContext(), getString(R.string.exportado_correcto, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), getString(R.string.error_exportar, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.reiniciarAnimaciones();
            adapter.notifyDataSetChanged();
        }
        cargarMovimientos();
    }


    // metodos de Firebase (cargar, eliminar, actualizar)


    private void cargarMovimientos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String correo = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaOriginal.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (correo.equals(ds.child("correo_electronico").getValue(String.class))) {
                        for (DataSnapshot mov : ds.child("movimientos").getChildren()) {
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

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.error_cargar_movimientos, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarMovimientoDeFirebase(Movimiento movimiento) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || movimiento.getKey() == null) return;
        String correo = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (correo.equals(ds.child("correo_electronico").getValue(String.class))) {
                        DatabaseReference userRef = ds.getRef();
                        userRef.child("movimientos").child(movimiento.getKey()).removeValue().addOnSuccessListener(aVoid -> {
                            double saldo = ds.child("saldo").getValue(Double.class) != null ? ds.child("saldo").getValue(Double.class) : 0.0;
                            double gastado = ds.child("saldo_gastado").getValue(Double.class) != null ? ds.child("saldo_gastado").getValue(Double.class) : 0.0;
                            double nuevoSaldo = movimiento.getTipo().equals("ingreso") ? saldo - movimiento.getMonto() : saldo + movimiento.getMonto();
                            double nuevoGastado = movimiento.getTipo().equals("retirar") ? gastado - movimiento.getMonto() : gastado;

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("saldo", nuevoSaldo);
                            updates.put("saldo_gastado", nuevoGastado);

                            userRef.updateChildren(updates).addOnCompleteListener(task -> {
                                Toast.makeText(getContext(), R.string.movimiento_eliminado, Toast.LENGTH_SHORT).show();
                                if (getActivity() instanceof MainActivity) {
                                    PrincipalFragment pf = ((MainActivity) getActivity()).getPrincipalFragment();
                                    if (pf != null && pf.isAdded()) pf.refrescarGrafico();
                                }
                            });
                        });
                        break;
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.error_eliminar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarTodosLosMovimientos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String correo = user.getEmail();

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (correo.equals(ds.child("correo_electronico").getValue(String.class))) {
                        DatabaseReference userRef = ds.getRef();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("movimientos", null);
                        updates.put("saldo", 0.0);
                        updates.put("saldo_gastado", 0.0);
                        userRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), R.string.todo_eliminado, Toast.LENGTH_SHORT).show();
                            cargarMovimientos();
                        });
                        break;
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.error_eliminar_todos, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
