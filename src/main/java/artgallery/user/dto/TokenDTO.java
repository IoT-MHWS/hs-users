package artgallery.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {

  @NotNull
  private String token;

  @NotNull
  @Min(value = 0L)
  private Long expiration; // millies
}
