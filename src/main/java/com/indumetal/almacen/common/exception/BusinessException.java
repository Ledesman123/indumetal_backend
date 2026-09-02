package com.indumetal.almacen.common.exception;

/**
 * Se lanza ante una violacion de una regla de negocio del almacen,
 * por ejemplo: intentar retirar mas stock del disponible.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
