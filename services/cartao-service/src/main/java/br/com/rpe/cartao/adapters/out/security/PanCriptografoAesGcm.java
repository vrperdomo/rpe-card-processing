package br.com.rpe.cartao.adapters.out.security;

import br.com.rpe.cartao.application.port.out.CriptografoPan;
import br.com.rpe.cartao.config.PanCriptografiaProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Component;

// Encryptors.stronger deriva a chave via PBKDF2 (senha+salt) e cifra com AES-GCM (autenticado,
// IV aleatorio por chamada) — evita reinventar cifra de bloco manualmente. pan_hash (SHA-256 +
// pepper) e independente da cifra: garante unicidade sem exigir decifrar todo o dataset.
@Component
public class PanCriptografoAesGcm implements CriptografoPan {

  private final BytesEncryptor encryptor;
  private final String pepper;

  public PanCriptografoAesGcm(PanCriptografiaProperties properties) {
    this.encryptor = Encryptors.stronger(properties.senha(), properties.salt());
    this.pepper = properties.pepper();
  }

  @Override
  public String cifrar(String panEmClaro) {
    byte[] cifrado = encryptor.encrypt(panEmClaro.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(cifrado);
  }

  @Override
  public String decifrar(String panCifrado) {
    byte[] decifrado = encryptor.decrypt(Base64.getDecoder().decode(panCifrado));
    return new String(decifrado, StandardCharsets.UTF_8);
  }

  @Override
  public String hash(String panEmClaro) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest((panEmClaro + pepper).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 não disponível", ex);
    }
  }
}
