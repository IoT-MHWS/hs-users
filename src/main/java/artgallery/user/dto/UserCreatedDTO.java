package artgallery.user.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedDTO {
  @NotNull
  private Long id;
  @NotNull
  private String login;
  @NotNull
  private String email;
}
