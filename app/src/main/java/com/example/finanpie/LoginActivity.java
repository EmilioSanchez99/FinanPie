package com.example.finanpie;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText email, password;
    private Button btnAcceder, btnAccederDirecto;
    private TextView txtCrearCuenta;
    private TextView contrasenaOlvidadaTextView;
    int cont = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();

        contrasenaOlvidadaTextView = findViewById(R.id.contrasenaOlvidadaTextView);
        contrasenaOlvidadaTextView.setVisibility(View.GONE);

        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etContrasena);
        btnAcceder = findViewById(R.id.btnAcceder);
        btnAccederDirecto = findViewById(R.id.btnAccderDirecto);
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta);

        btnAccederDirecto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailAdmin = "admin@gmail.com";
                String passwordAdmin = "123456";

                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailAdmin, passwordAdmin)
                        .addOnCompleteListener(LoginActivity.this, task -> {
                            if (task.isSuccessful()) {
                                // Login exitoso, redirigir al MainActivity
                                Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                                intent.putExtra("email", emailAdmin);
                                startActivity(intent);
                                finish();
                            } else {
                                // Error en el login
                                Toast.makeText(LoginActivity.this, "Error al acceder como admin.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


        btnAcceder.setOnClickListener(view -> {
            String correo = email.getText().toString().trim();
            String contrasena = password.getText().toString().trim();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            } else {
                signIn(correo, contrasena);
            }

            cont++;
            if (cont == 3) {
                contrasenaOlvidadaTextView.setVisibility(View.VISIBLE);
                contrasenaOlvidadaTextView.setOnClickListener(v -> restablecerContrasena());
            }
        });

        txtCrearCuenta.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, CrearCuentaActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.signOut(); 
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario autenticado: " + currentUser.getEmail());
        }
    }

    private void signIn(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Bienvenido: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error inesperado: usuario nulo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Fallo en la autenticación. Verifica tu email y contraseña.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void restablecerContrasena() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Restablecer contraseña");
        builder.setMessage("Ingresa tu correo electrónico para recibir un enlace de restablecimiento.");

        final EditText input = new EditText(LoginActivity.this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Correo electrónico");
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Correo enviado. Revisa tu bandeja de entrada.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Error al enviar el correo.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Por favor, ingresa un correo válido.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
