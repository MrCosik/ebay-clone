package pl.edu.pjwstk.jaz.zad2;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class AppAuthentication extends AbstractAuthenticationToken {

    private final User authenticatedUser;

    public AppAuthentication(User authenticatedUser){
        super(toGrantedAuthorities(authenticatedUser.getRoles()));
        this.authenticatedUser = authenticatedUser;
        setAuthenticated(true);
    }

    public static Collection<? extends GrantedAuthority> toGrantedAuthorities(Set<String> roles){
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return authenticatedUser;
    }
}