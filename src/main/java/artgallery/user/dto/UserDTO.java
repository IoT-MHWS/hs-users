package artgallery.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
  @NotNull
  @Size(max = 32, message = "The login '${validatedValue}' must less than {max} characters long")
  private String login;

  @NotNull
  @Size(max = 255, message = "The password must less than {max} characters long")
  private String password;

  @NotNull
  @Size(max = 255, message = "The email must less than {max} characters long")
  @Email(message = "The email is not correct")
  private String email;
}
