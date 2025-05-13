package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanpie.Meta;
import com.example.finanpie.MetaAdapter;
import com.example.finanpie.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObjetivosFragment extends Fragment {

    private RecyclerView rvMetas;
    private FloatingActionButton fabAgregarMeta;
    private List<Meta> listaMetas;
    private MetaAdapter adapter;
    private DatabaseReference usuariosRef;
    private FirebaseAuth mAuth;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_objetivos, container, false);

        rvMetas = view.findViewById(R.id.rvMetas);
        fabAgregarMeta = view.findViewById(R.id.fabAgregarMeta);

        listaMetas = new ArrayList<>();
        adapter = new MetaAdapter(listaMetas, requireContext());
        rvMetas.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMetas.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Meta meta = listaMetas.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar meta")
                        .setMessage("¿Estás seguro de que quieres eliminar esta meta?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            eliminarMetaDeFirebase(meta);
                            listaMetas.remove(position);
                            adapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // restaurar si cancela
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }
        }).attachToRecyclerView(rvMetas);


        fabAgregarMeta.setOnClickListener(v -> mostrarDialogoCrearMeta());
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app");
        usuariosRef = database.getReference("usuarios");

        cargarMetasDesdeFirebase(); // 🔁 carga automática

        return view;
    }

    private void eliminarMetaDeFirebase(Meta meta) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();

        FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                            String correo = usuarioSnap.child("correo_electronico").getValue(String.class);
                            String key = usuarioSnap.getKey();

                            if (correo != null && correo.equals(correoUsuario)) {
                                usuarioSnap.getRef()
                                        .child("metas")
                                        .child(meta.getId())
                                        .removeValue();
                            }

                            if ("admin".equals(key)) {
                                usuarioSnap.getRef()
                                        .child("metas")
                                        .child(meta.getId())
                                        .removeValue();
                            }
                        }

                        Toast.makeText(requireContext(), "Meta eliminada", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Error al eliminar la meta", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void guardarMetaEnFirebase(Meta meta) {
        Log.d("ObjetivosFragment", "Iniciando guardarMetaEnFirebase");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("ObjetivosFragment", "Usuario no autenticado");
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String correoUsuario = user.getEmail();
        Log.d("ObjetivosFragment", "Correo del usuario autenticado: " + correoUsuario);

        FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("ObjetivosFragment", "onDataChange ejecutado con " + snapshot.getChildrenCount() + " usuarios");

                        DataSnapshot nodoUsuario = null;
                        DataSnapshot nodoAdmin = null;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String correo = ds.child("correo_electronico").getValue(String.class);
                            Log.d("ObjetivosFragment", "Revisando usuario: " + ds.getKey() + " con correo: " + correo);

                            if (correo != null && correo.equals(correoUsuario)) {
                                nodoUsuario = ds;
                                Log.d("ObjetivosFragment", "Nodo usuario identificado: " + ds.getKey());
                            }
                            if ("admin".equals(ds.getKey())) {
                                nodoAdmin = ds;
                                Log.d("ObjetivosFragment", "Nodo admin identificado");
                            }
                        }

                        if (nodoUsuario == null) {
                            Log.e("ObjetivosFragment", "No se encontró el nodo del usuario");
                            Toast.makeText(requireContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Guardar en el nodo del usuario autenticado
                        nodoUsuario.getRef().child("metas").child(meta.getId()).setValue(meta)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("ObjetivosFragment", "Meta guardada en nodo usuario correctamente");
                                    } else {
                                        Log.e("ObjetivosFragment", "Error guardando meta en nodo usuario", task.getException());
                                    }
                                });

                        // Guardar también en el nodo admin
                        if (nodoAdmin != null) {
                            nodoAdmin.getRef().child("metas").child(meta.getId()).setValue(meta)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d("ObjetivosFragment", "Meta guardada en nodo admin correctamente");
                                        } else {
                                            Log.e("ObjetivosFragment", "Error guardando meta en nodo admin", task.getException());
                                        }
                                    });
                        }

                        Toast.makeText(requireContext(), "Meta guardada correctamente", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ObjetivosFragment", "Error de Firebase: " + error.getMessage(), error.toException());
                        Toast.makeText(requireContext(), "Error de Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void cargarMetasDesdeFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("ObjetivosFragment", "Usuario no autenticado");
            return;
        }

        String correoUsuario = user.getEmail();
        Log.d("ObjetivosFragment", "Cargando metas para: " + correoUsuario);

        FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listaMetas.clear();

                        for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                            String correo = usuarioSnap.child("correo_electronico").getValue(String.class);
                            Log.d("ObjetivosFragment", "Revisando: " + usuarioSnap.getKey() + " → " + correo);

                            if (correo != null && correo.equals(correoUsuario)) {
                                Log.d("ObjetivosFragment", "Cargando metas desde: " + usuarioSnap.getKey());

                                for (DataSnapshot metaSnap : usuarioSnap.child("metas").getChildren()) {
                                    Meta meta = metaSnap.getValue(Meta.class);
                                    if (meta != null) {
                                        listaMetas.add(meta);
                                        Log.d("ObjetivosFragment", "Meta cargada: " + meta.getNombre());
                                    }
                                }

                                adapter.notifyDataSetChanged();
                                return;
                            }
                        }

                        Log.w("ObjetivosFragment", "No se encontró el nodo del usuario para cargar metas");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ObjetivosFragment", "Error al cargar metas: " + error.getMessage(), error.toException());
                    }
                });
    }




    private void mostrarDialogoCrearMeta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Nueva meta");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_meta, null);
        EditText etNombre = dialogView.findViewById(R.id.etNombreMetaDialog);
        EditText etObjetivo = dialogView.findViewById(R.id.etObjetivoMetaDialog);

        builder.setView(dialogView);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String objetivoStr = etObjetivo.getText().toString().trim();

            if (nombre.isEmpty() || objetivoStr.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double cantidadObjetivo = Double.parseDouble(objetivoStr);
                String id = UUID.randomUUID().toString();
                Meta nuevaMeta = new Meta(id, nombre, cantidadObjetivo);

                listaMetas.add(nuevaMeta);
                adapter.notifyItemInserted(listaMetas.size() - 1);
                guardarMetaEnFirebase(nuevaMeta); // 🔥 Guardar en Firebase

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Cantidad inválida", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }


}
