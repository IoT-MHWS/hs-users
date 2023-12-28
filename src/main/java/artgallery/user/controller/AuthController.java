package artgallery.user.controller;

import artgallery.user.dto.TokenDTO;
import artgallery.user.dto.UserDTO;
import artgallery.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<TokenDTO>> login(@RequestBody @Valid UserDTO userDTO) {
    return authService.login(userDTO)
      .map(jwt -> new ResponseEntity<>(jwt, HttpStatus.OK));
  }
}
