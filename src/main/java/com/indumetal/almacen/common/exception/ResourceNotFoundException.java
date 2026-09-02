package com.indumetal.almacen.common.exception;

/** Se lanza cuando una entidad solicitada (material, ubicacion, usuario, etc.) no existe. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entidad, Object id) {
        return new ResourceNotFoundException(entidad + " no encontrado con id: " + id);
    }
}
