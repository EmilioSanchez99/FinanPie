package com.example.finanpie;

public class Meta {
    private String id;
    private String nombre;
    private double objetivo;
    private double acumulado; // 🔥 nuevo campo

    public Meta() {
        // Requerido por Firebase
    }

    public Meta(String id, String nombre, double objetivo) {
        this.id = id;
        this.nombre = nombre;
        this.objetivo = objetivo;
        this.acumulado = 0.0; // valor por defecto
    }

    // GETTERS Y SETTERS

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(double objetivo) {
        this.objetivo = objetivo;
    }

    public double getAcumulado() {
        return acumulado;
    }

    public void setAcumulado(double acumulado) {
        this.acumulado = acumulado;
    }
}
