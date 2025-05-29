package com.example.finanpie;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.finanpie.TabFragments.PrincipalFragment;
import com.example.finanpie.TabFragments.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ViewPagerAdapter adapter; // Usa esta
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tablayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        adapter = new ViewPagerAdapter(this); // Asigna a la variable global
        viewPager.setAdapter(adapter);




        new TabLayoutMediator(tablayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.ic_home);
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_objetivos);
                    break;
                case 2:
                    tab.setIcon(R.drawable.ic_historial);
                    break;
                case 3:
                    tab.setIcon(R.drawable.ic_perfil);
                    break;
            }
        }).attach();
    }


    // 🔁 Permitir a otros fragments acceder a PrincipalFragment
    public PrincipalFragment getPrincipalFragment() {
        return (PrincipalFragment) adapter.getFragment(0); // posición 0 = PrincipalFragment
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            signOut();
            return true;
        } else if (id == R.id.cambiar_idioma) {
            cambiarIdioma("en");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cambiarIdioma(String lenguaje) {
        Locale locale = new Locale(lenguaje);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        Intent refresh = new Intent(this, this.getClass());
        refresh.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(refresh);
        finish();
    }

}
