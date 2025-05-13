package com.example.finanpie;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Asociar vistas del diseño XML
        etEmail = findViewById(R.id.etEmailCrearCuenta);
        etPassword = findViewById(R.id.etContrasenaCrearCuenta);
        etNombre = findViewById(R.id.etNombreCrearCuenta);
        etApellido = findViewById(R.id.etApellidoCrearCuenta);
        etEdad = findViewById(R.id.etEdadCrearCuenta);
        etSaldoInicial = findViewById(R.id.etSaldoInicialCrearCuenta);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        // Listener para el botón "Crear Cuenta"
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());
    }

    private void crearCuenta() {
        // Obtener datos de entrada
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String edadStr = etEdad.getText().toString().trim();
        String saldoStr = etSaldoInicial.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || edadStr.isEmpty() || saldoStr.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        int edad;
        double saldoInicial;
        try {
            edad = Integer.parseInt(edadStr); // Convertir edad a entero
            saldoInicial = Double.parseDouble(saldoStr); // Convertir saldo inicial a decimal
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor, ingrese valores numéricos válidos para edad y saldo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear cuenta en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Éxito en la creación de la cuenta
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Añadir el usuario a la base de datos
                            agregarUsuarioABaseDeDatos(user, nombre, apellido, email, edad, saldoInicial);
                            finish();
                            Toast.makeText(CrearCuentaActivity.this, "C uenta creada exitosamente.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Error en la creación de la cuenta
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CrearCuentaActivity.this, "Fallo al crear la cuenta: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void agregarUsuarioABaseDeDatos(FirebaseUser user, String nombre, String apellido, String email, int edad, double saldoInicial) {
        if (user == null) return;

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://finanpie-a39a2-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference usuariosRef = database.getReference("usuarios");

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long cantidadUsuarios = snapshot.getChildrenCount();
                String nuevoId = "user" + (cantidadUsuarios + 1);

                // Crear estructura manual con los campos deseados
                DatabaseReference nuevoUsuarioRef = usuariosRef.child(nuevoId);

                Map<String, Object> datosUsuario = new HashMap<>();
                datosUsuario.put("id", nuevoId);
                datosUsuario.put("nombre", nombre);
                datosUsuario.put("apellido", apellido);
                datosUsuario.put("correo_electronico", email);
                datosUsuario.put("edad", edad);
                datosUsuario.put("saldo", saldoInicial);
                datosUsuario.put("saldo_gastado", 0.0);
                datosUsuario.put("movimientos", new HashMap<>()); // vacío al inicio

                nuevoUsuarioRef.setValue(datosUsuario)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(CrearCuentaActivity.this, "Usuario añadido a la base de datos.", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "Error al añadir usuario a la base de datos.", task.getException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al leer los usuarios existentes.", error.toException());
            }
        });
    }




    @Override
    public void onStart() {
        super.onStart();
        // Comprobar si hay un usuario autenticado actualmente
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario actualmente autenticado: " + currentUser.getEmail());
        }
    }
}
