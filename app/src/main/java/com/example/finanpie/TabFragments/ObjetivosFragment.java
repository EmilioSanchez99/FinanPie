package com.example.finanpie.TabFragments;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.google.firebase.database.*;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_objetivos, container, false);

        initViews(view);
        setupRecyclerView();
        setupSwipeToDelete();
        setupFirebase();
        setupListeners();

        cargarMetasDesdeFirebase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.reiniciarAnimaciones();
            adapter.notifyDataSetChanged();
        }
    }


    private void initViews(View view) {
        rvMetas = view.findViewById(R.id.rvMetas);
        fabAgregarMeta = view.findViewById(R.id.fabAgregarMeta);
    }

    private void setupRecyclerView() {
        listaMetas = new ArrayList<>();
        adapter = new MetaAdapter(listaMetas, requireContext());
        rvMetas.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMetas.setAdapter(adapter);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");
    }

    private void setupListeners() {
        fabAgregarMeta.setOnClickListener(v -> mostrarDialogoCrearMeta());
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Meta meta = listaMetas.get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.dialog_titulo_eliminar)
                        .setMessage(R.string.dialog_mensaje_eliminar)
                        .setPositiveButton(R.string.boton_si, (dialog, which) -> {
                            eliminarMetaDeFirebase(meta);
                            listaMetas.remove(position);
                            adapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton(R.string.boton_cancelar, (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }
        }).attachToRecyclerView(rvMetas);
    }

    private void mostrarDialogoCrearMeta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.titulo_nueva_meta);

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_meta, null);
        EditText etNombre = dialogView.findViewById(R.id.etNombreMetaDialog);
        EditText etObjetivo = dialogView.findViewById(R.id.etObjetivoMetaDialog);

        etNombre.setHint(R.string.hint_nombre_meta);
        etObjetivo.setHint(R.string.hint_cantidad_objetivo);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.btn_crear, (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String objetivoStr = etObjetivo.getText().toString().trim();

            if (nombre.isEmpty() || objetivoStr.isEmpty()) {
                Toast.makeText(getContext(), R.string.toast_completa_campos, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double cantidadObjetivo = Double.parseDouble(objetivoStr);
                Meta nuevaMeta = new Meta(UUID.randomUUID().toString(), nombre, cantidadObjetivo);

                listaMetas.add(nuevaMeta);
                adapter.notifyItemInserted(listaMetas.size() - 1);
                guardarMetaEnFirebase(nuevaMeta);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), R.string.toast_cantidad_invalida, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.btn_cancelar, null);
        builder.show();
    }

    private void guardarMetaEnFirebase(Meta meta) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), R.string.usuario_no_encontrado, Toast.LENGTH_SHORT).show();
            return;
        }

        String correoUsuario = user.getEmail();
        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot nodoUsuario = null, nodoAdmin = null;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) nodoUsuario = ds;
                    if ("admin".equals(ds.getKey())) nodoAdmin = ds;
                }

                if (nodoUsuario == null) {
                    Toast.makeText(requireContext(), R.string.usuario_no_encontrado, Toast.LENGTH_SHORT).show();
                    return;
                }

                nodoUsuario.getRef().child("metas").child(meta.getId()).setValue(meta);
                if (nodoAdmin != null) nodoAdmin.getRef().child("metas").child(meta.getId()).setValue(meta);
                Toast.makeText(requireContext(), R.string.toast_meta_guardada, Toast.LENGTH_SHORT).show();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Firebase error" + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void eliminarMetaDeFirebase(Meta meta) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();
        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                    String correo = usuarioSnap.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario) || "admin".equals(usuarioSnap.getKey())) {
                        usuarioSnap.getRef().child("metas").child(meta.getId()).removeValue();
                    }
                }
                Toast.makeText(requireContext(), R.string.toast_meta_eliminada, Toast.LENGTH_SHORT).show();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), R.string.error_eliminar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarMetasDesdeFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String correoUsuario = user.getEmail();
        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMetas.clear();
                for (DataSnapshot usuarioSnap : snapshot.getChildren()) {
                    String correo = usuarioSnap.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(correoUsuario)) {
                        for (DataSnapshot metaSnap : usuarioSnap.child("metas").getChildren()) {
                            Meta meta = metaSnap.getValue(Meta.class);
                            if (meta != null) listaMetas.add(meta);
                        }
                        adapter.notifyDataSetChanged();
                        return;
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error al cargar las metas", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
