package com.example.finanpie;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText email, password;
    private Button btnAcceder, btnAccederDirecto;
    private TextView txtCrearCuenta, contrasenaOlvidadaTextView;
    private int cont = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etContrasena);
        btnAcceder = findViewById(R.id.btnAcceder);
        btnAccederDirecto = findViewById(R.id.btnAccderDirecto);
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta);
        contrasenaOlvidadaTextView = findViewById(R.id.contrasenaOlvidadaTextView);
        contrasenaOlvidadaTextView.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnAcceder.setOnClickListener(view -> {
            String correo = email.getText().toString().trim();
            String contrasena = password.getText().toString().trim();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            } else {
                signIn(correo, contrasena);
            }

            cont++;
            if (cont == 3) {
                contrasenaOlvidadaTextView.setVisibility(View.VISIBLE);
                contrasenaOlvidadaTextView.setOnClickListener(v -> mostrarDialogoRestablecerContrasena());
            }
        });

        btnAccederDirecto.setOnClickListener(view -> accesoAdminDirecto());
        txtCrearCuenta.setOnClickListener(v -> startActivity(new Intent(this, CrearCuentaActivity.class)));
    }

    private void accesoAdminDirecto() {
        String emailAdmin = "admin@gmail.com";
        String passwordAdmin = "123456";

        mAuth.signInWithEmailAndPassword(emailAdmin, passwordAdmin)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        iniciarApp(emailAdmin);
                    } else {
                        Toast.makeText(this, "Error al acceder como admin.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signIn(String correo, String contrasena) {
        mAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(this, "Bienvenido: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            iniciarApp(correo);
                        } else {
                            Toast.makeText(this, "Error inesperado: usuario nulo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Fallo en la autenticación. Verifica tu email y contraseña.", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                    }
                });
    }

    private void iniciarApp(String correo) {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("email", correo);
        startActivity(intent);
        finish();
    }

    private void mostrarDialogoRestablecerContrasena() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restablecer contraseña");
        builder.setMessage("Ingresa tu correo electrónico para recibir un enlace de restablecimiento.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Correo electrónico");
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Correo enviado. Revisa tu bandeja de entrada.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error al enviar el correo.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Por favor, ingresa un correo válido.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.signOut(); // Forzar logout al abrir pantalla
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario autenticado: " + currentUser.getEmail());
        }
    }
}
