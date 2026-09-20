package br.com.rpe.cartao.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// senha/salt alimentam Encryptors.gcm (AES-GCM); pepper e concatenado ao PAN antes do SHA-256
// para o hash de unicidade — nenhum dos tres pode ser reconstruido a partir do banco sozinho.
@ConfigurationProperties(prefix = "rpe.cartao.pan-criptografia")
@Validated
public record PanCriptografiaProperties(
    @NotBlank String senha, @NotBlank String salt, @NotBlank String pepper) {}
