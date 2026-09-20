package br.com.rpe.portador.adapters.in.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

  public static LoginResponse deToken(String accessToken, long expiresIn) {
    return new LoginResponse(accessToken, "Bearer", expiresIn);
  }
}
