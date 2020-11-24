package pl.edu.pjwstk.jaz.zad2;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    private final AuthenticationService authenticationService;

    public LoginController(AuthenticationService authenticationService){
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest loginRequest) throws UnauthorizedException {
        var isLogged = authenticationService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if(!isLogged){
            throw new UnauthorizedException();
        }
    }
}
