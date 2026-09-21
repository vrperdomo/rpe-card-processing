package br.com.rpe.portador.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Guarda do messages.properties (issue #119): a API responde em português e não em inglês. Sem
 * mensagem própria para uma restrição do Bean Validation, o Hibernate Validator cai no bundle dele,
 * que é inglês, e o cliente volta a ver "must not be blank".
 */
class MensagensEmPortuguesTest {

  private static Properties mensagens() throws Exception {
    Properties propriedades = new Properties();
    try (InputStream in =
        MensagensEmPortuguesTest.class.getResourceAsStream("/messages.properties")) {
      assertThat(in).as("messages.properties no classpath").isNotNull();
      propriedades.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return propriedades;
  }

  // Descobre as restrições no próprio jar da API: uma restrição nova numa versão futura do Jakarta
  // Validation faz este teste falhar até ganhar tradução, em vez de virar inglês em silêncio.
  private static List<Class<?>> restricoesPadrao() throws Exception {
    List<Class<?>> tipos = new ArrayList<>();
    Resource[] classes =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:jakarta/validation/constraints/*.class");
    for (Resource recurso : classes) {
      String nome = recurso.getFilename();
      if (nome == null || nome.contains("$") || nome.startsWith("package-info")) {
        continue;
      }
      Class<?> tipo = Class.forName("jakarta.validation.constraints." + nome.replace(".class", ""));
      if (tipo.isAnnotation() && temMetodoMessage(tipo)) {
        tipos.add(tipo);
      }
    }
    return tipos;
  }

  private static boolean temMetodoMessage(Class<?> tipo) {
    try {
      tipo.getMethod("message");
      return true;
    } catch (NoSuchMethodException ex) {
      return false;
    }
  }

  @Test
  void todaRestricaoPadraoDoBeanValidationDeveTerMensagemEmPortugues() throws Exception {
    Properties mensagens = mensagens();
    List<Class<?>> restricoes = restricoesPadrao();
    List<String> semMensagem = new ArrayList<>();

    for (Class<?> restricao : restricoes) {
      String chave = "jakarta.validation.constraints." + restricao.getSimpleName() + ".message";
      String texto = mensagens.getProperty(chave);
      if (texto == null || texto.isBlank()) {
        semMensagem.add(chave);
      }
    }

    assertThat(restricoes)
        .as("a descoberta das restrições precisa achar as do Jakarta")
        .hasSizeGreaterThanOrEqualTo(22);
    assertThat(semMensagem).as("restrições sem tradução").isEmpty();
  }

  @Test
  void mensagemPadraoDeCadaRestricaoDeveApontarParaAChaveTraduzida() throws Exception {
    for (Class<?> restricao : restricoesPadrao()) {
      Object padrao = restricao.getMethod("message").getDefaultValue();
      assertThat(padrao)
          .as("mensagem padrão de %s", restricao.getSimpleName())
          .isEqualTo("{jakarta.validation.constraints." + restricao.getSimpleName() + ".message}");
    }
  }

  @Test
  void naoDeveHaverMensagemTraduzidaVazia() throws Exception {
    mensagens()
        .forEach((chave, texto) -> assertThat(texto.toString()).as(chave.toString()).isNotBlank());
  }
}
