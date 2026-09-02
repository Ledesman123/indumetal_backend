package com.indumetal.almacen.common.exception;

/** Se lanza cuando las credenciales son invalidas o el usuario no tiene permisos. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
