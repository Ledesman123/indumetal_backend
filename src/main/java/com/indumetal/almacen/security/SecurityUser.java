package com.indumetal.almacen.security;

import com.indumetal.almacen.modules.usuario.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Envuelve la entidad Usuario para que Spring Security la entienda. */
@Getter
public class SecurityUser implements UserDetails {

    private final Usuario usuario;

    public SecurityUser(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefijo "ROLE_" requerido por hasRole("...") de Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo();
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.getBloqueadoHasta() == null || usuario.getBloqueadoHasta().isBefore(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getActivo());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
