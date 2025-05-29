package com.example.finanpie.TabFragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.finanpie.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PerfilFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgPerfil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        imgPerfil = view.findViewById(R.id.imgPerfil);
        TextView txtEditarPerfil = view.findViewById(R.id.txtEditarPerfil);
        CardView recomendarCard = view.findViewById(R.id.recomendarAmigos);
        CardView premiumCard = view.findViewById(R.id.premium);

        txtEditarPerfil.setOnClickListener(v -> abrirGaleria());

        premiumCard.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Hazte Premium")
                    .setMessage("Desbloquea funciones exclusivas como estadísticas avanzadas, sin anuncios y más.")
                    .setPositiveButton("Comprar", (dialog, which) -> {
                        Toast.makeText(getContext(), "Función aún no disponible", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        recomendarCard.setOnClickListener(v -> {
            try {
                Uri smsUri = Uri.parse("smsto:");
                Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
                intent.putExtra("sms_body", "¡Descubre FinanPie! Tu nueva app de finanzas personales 🤑📱");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No se pudo abrir el panel de mensajes", Toast.LENGTH_SHORT).show();
            }
        });

        cargarImagenPerfil();
        return view;
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            Log.d("PerfilFragment", "Imagen seleccionada: " + selectedImageUri);
            subirImagenAFirebase(selectedImageUri);
        }
    }

    private void subirImagenAFirebase(Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || imageUri == null) {
            Log.e("PerfilFragment", "Usuario o URI inválido");
            return;
        }

        String userId = user.getUid();
        StorageReference storageRef = FirebaseStorage
                .getInstance("gs://finanpie-a39a2.firebasestorage.app") // Cambia al tuyo si es distinto
                .getReference()
                .child("imagenes_perfil/" + userId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    guardarUrlEnRealtimeDatabase(uri.toString());
                    imgPerfil.setImageURI(imageUri);
                    Log.d("PerfilFragment", "Imagen subida con éxito. URL: " + uri.toString());
                    Toast.makeText(getContext(), getString(R.string.imagen_subida_exitosamente), Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    Log.e("PerfilFragment", "Error al subir imagen", e);
                    Toast.makeText(getContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarUrlEnRealtimeDatabase(String urlImagen) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String emailUsuario = user.getEmail();
        DatabaseReference usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(emailUsuario)) {
                        ds.getRef().child("foto_url").setValue(urlImagen);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PerfilFragment", "Error al guardar URL en la base de datos", error.toException());
            }
        });
    }

    private void cargarImagenPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String emailUsuario = user.getEmail();
        DatabaseReference usuariosRef = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String correo = ds.child("correo_electronico").getValue(String.class);
                    if (correo != null && correo.equals(emailUsuario)) {
                        String url = ds.child("foto_url").getValue(String.class);
                        if (url != null && !url.isEmpty()) {
                            Log.d("PerfilFragment", "Cargando imagen desde URL: " + url);
                            Glide.with(requireContext()).load(url).into(imgPerfil);
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PerfilFragment", "Error al cargar imagen", error.toException());
            }
        });
    }
}
