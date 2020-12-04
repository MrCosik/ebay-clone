package pl.edu.pjwstk.jaz;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import pl.edu.pjwstk.jaz.zad2.AuthenticationService;
import pl.edu.pjwstk.jaz.zad2.LoginRequest;
import pl.edu.pjwstk.jaz.zad2.RegisterRequest;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@RunWith(SpringRunner.class)
@IntegrationTest
public class AccessTest {

    @BeforeClass
    public static void register_user(){
        given()
                .body(new RegisterRequest("user","user"))
                .contentType(ContentType.JSON)
                .post("/api/register");
    }

    @Test
    public void logged_admin_should_be_allowed_to_enter_edit(){
        var response = given()
                .body(new LoginRequest("admin", "admin"))
                .contentType(ContentType.JSON)
                .post("/api/login")
                .thenReturn();

        given()
                .cookies(response.getCookies())
                .get("/api/edit")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void logged_admin_should_be_allowed_to_enter_explore(){
        var response = given()
                .body(new LoginRequest("admin", "admin"))
                .contentType(ContentType.JSON)
                .post("/api/login")
                .thenReturn();

        given()
                .cookies(response.getCookies())
                .get("/api/explore")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void user_should_access_explore(){
        var response = given()
                .body(new LoginRequest("user", "user"))
                .contentType(ContentType.JSON)
                .post("/api/login")
                .thenReturn();

        given()
                .cookies(response.getCookies())
                .get("/api/explore")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void user_should_not_access_edit(){
        var response = given()
                .body(new LoginRequest("user", "user"))
                .contentType(ContentType.JSON)
                .post("/api/login")
                .thenReturn();

        given()
                .cookies(response.getCookies())
                .get("/api/edit")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }






}