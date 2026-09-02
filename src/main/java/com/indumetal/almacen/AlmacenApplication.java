package com.indumetal.almacen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada del backend de la Intranet de Gestion de Almacen
 * de INDUMETAL PERU S.A.C.
 *
 * Arquitectura: API REST en capas (Controller -> Service -> Repository -> Entity),
 * organizada por modulo de negocio (package by feature), con seguridad JWT
 * y persistencia en Supabase (PostgreSQL) gestionada con Flyway.
 */
@SpringBootApplication
@EnableScheduling // habilita jobs programados (ej. alerta de stock minimo)
public class AlmacenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlmacenApplication.class, args);
    }
}
