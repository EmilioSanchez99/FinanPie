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
import android.widget.RatingBar;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        inicializarVistas(view);
        cargarImagenPerfil();

        return view;
    }

    private void inicializarVistas(View view) {
        imgPerfil = view.findViewById(R.id.imgPerfil);
        TextView txtEditarPerfil = view.findViewById(R.id.txtEditarPerfil);
        CardView recomendarCard = view.findViewById(R.id.recomendarAmigos);
        CardView premiumCard = view.findViewById(R.id.premium);
        CardView calificarCard = view.findViewById(R.id.carlificar);
        CardView bloquearAnunciosCard = view.findViewById(R.id.bloquear_anuncios);
        CardView ajustesCard = view.findViewById(R.id.aboutUs);
        CardView contactoCard = view.findViewById(R.id.contacto);

        txtEditarPerfil.setOnClickListener(v -> abrirGaleria());
        recomendarCard.setOnClickListener(v -> compartirPorSMS());
        premiumCard.setOnClickListener(v -> mostrarDialogoPremium());
        bloquearAnunciosCard.setOnClickListener(v -> mostrarDialogoBloquearAnuncios());
        calificarCard.setOnClickListener(v -> mostrarDialogoCalificacion());
        ajustesCard.setOnClickListener(v -> mostrarVentanaSobreNosotros());
        contactoCard.setOnClickListener(v -> enviarCorreo());
    }

    private void compartirPorSMS() {
        try {
            Uri smsUri = Uri.parse("smsto:");
            Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
            intent.putExtra("sms_body", "¡Descubre FinanPie! Tu nueva app de finanzas personales 🤑📱");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "No se pudo abrir el panel de mensajes", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoPremium() {
        new AlertDialog.Builder(getContext())
                .setTitle("Hazte Premium")
                .setMessage("Desbloquea funciones exclusivas como estadísticas avanzadas, sin anuncios y más.")
                .setPositiveButton("Comprar", (dialog, which) -> {
                    Toast.makeText(getContext(), "Función aún no disponible", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoBloquearAnuncios() {
        new AlertDialog.Builder(getContext())
                .setTitle("Desbloquea los anuncios")
                .setMessage("Elimina todos los anuncios sin necesidad de ser Premium!")
                .setPositiveButton("Comprar", (dialog, which) -> {
                    Toast.makeText(getContext(), "Función aún no disponible", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoCalificacion() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_calificacion, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        new AlertDialog.Builder(getContext())
                .setTitle("Califica la app")
                .setView(dialogView)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    Toast.makeText(getContext(), "Gracias por tu valoración: " + rating + " estrellas", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarVentanaSobreNosotros() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sobre_nosotros, null);

        TextView txtIntegrantes = dialogView.findViewById(R.id.txtIntegrantes);
        TextView txtWeb = dialogView.findViewById(R.id.txtWeb);

        String integrantes =
                "Emilio Sánchez Vargas – 25 años\n" +
                        "Carlos Fernández Cano – 25 años\n" +
                        "Javier Marín Trujillo – 25 años\n" +
                        "Víctor Vera Rodrigues – 25 años";

        txtIntegrantes.setText(integrantes);
        txtWeb.setText("Visita nuestra web");
        txtWeb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.medac.es.com"));
            startActivity(intent);
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Equipo FinanPie")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void enviarCorreo() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:finanpie.app@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Contacto desde la app FinanPie");
        intent.putExtra(Intent.EXTRA_TEXT, "Hola,!");

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo con..."));
        } catch (Exception e) {
            Toast.makeText(getContext(), "No se pudo abrir el cliente de correo", Toast.LENGTH_SHORT).show();
        }
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
            Toast.makeText(getContext(), getString(R.string.imagen_subida_exitosamente), Toast.LENGTH_SHORT).show();
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
                .getInstance()
                .getReference("imagenes_perfil/" + userId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    guardarUrlEnRealtimeDatabase(uri.toString());
                    imgPerfil.setImageURI(imageUri);
                    Log.d("PerfilFragment", "Imagen subida con éxito. URL: " + uri);
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

        Toast.makeText(getContext(), R.string.cargando_imagen, Toast.LENGTH_SHORT).show();
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
