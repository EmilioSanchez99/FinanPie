package com.example.finanpie;

public class Movimiento {
    private String tipo;
    private double monto;
    private String fecha;
    private String key;

    public Movimiento() {}

    public Movimiento(String tipo, double monto, String fecha) {
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
    }

    public String getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public String getFecha() { return fecha; }
    public String getKey() { return key; }
    public void setKey(String key) {
        this.key = key;
    }
}
