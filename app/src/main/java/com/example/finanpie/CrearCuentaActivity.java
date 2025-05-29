package com.example.finanpie;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class CrearCuentaActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword, etNombre, etApellido, etEdad, etSaldoInicial;
    private Button btnCrearCuenta;
    private static final String TAG = "CrearCuentaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_cuenta);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmailCrearCuenta);
        etPassword = findViewById(R.id.etContrasenaCrearCuenta);
        etNombre = findViewById(R.id.etNombreCrearCuenta);
        etApellido = findViewById(R.id.etApellidoCrearCuenta);
        etEdad = findViewById(R.id.etEdadCrearCuenta);
        etSaldoInicial = findViewById(R.id.etSaldoInicialCrearCuenta);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
    }

    private void setupListeners() {
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());
    }

    private void crearCuenta() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String edadStr = etEdad.getText().toString().trim();
        String saldoStr = etSaldoInicial.getText().toString().trim();

        if (!validarCampos(email, password, nombre, apellido, edadStr, saldoStr)) return;

        int edad = Integer.parseInt(edadStr);
        double saldoInicial = Double.parseDouble(saldoStr);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarUsuarioEnFirebase(user, nombre, apellido, email, edad, saldoInicial);
                        Toast.makeText(this, "Cuenta creada exitosamente.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.w(TAG, "Error al crear cuenta", task.getException());
                        Toast.makeText(this, "Fallo: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validarCampos(String email, String password, String nombre, String apellido, String edadStr, String saldoStr) {
        if (email.isEmpty() || password.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || edadStr.isEmpty() || saldoStr.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Integer.parseInt(edadStr);
            Double.parseDouble(saldoStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ingrese valores numéricos válidos para edad y saldo.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void guardarUsuarioEnFirebase(FirebaseUser user, String nombre, String apellido, String email, int edad, double saldoInicial) {
        if (user == null) return;

        DatabaseReference usuariosRef = FirebaseDatabase
                .getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalUsuarios = snapshot.getChildrenCount();
                String nuevoId = "user" + (totalUsuarios + 1);
                DatabaseReference nuevoUsuarioRef = usuariosRef.child(nuevoId);

                Map<String, Object> datosUsuario = new HashMap<>();
                datosUsuario.put("id", nuevoId);
                datosUsuario.put("nombre", nombre);
                datosUsuario.put("apellido", apellido);
                datosUsuario.put("correo_electronico", email);
                datosUsuario.put("edad", edad);
                datosUsuario.put("saldo", saldoInicial);
                datosUsuario.put("saldo_gastado", 0.0);
                datosUsuario.put("movimientos", new HashMap<>()); // vacío inicialmente

                nuevoUsuarioRef.setValue(datosUsuario)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(CrearCuentaActivity.this, "Usuario añadido a la base de datos.", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "Error al guardar usuario en Firebase.", task.getException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al acceder a Firebase", error.toException());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario autenticado previamente: " + currentUser.getEmail());
        }
    }
}
