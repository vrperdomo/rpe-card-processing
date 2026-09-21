package br.com.rpe.cartao;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// Verifica as fronteiras hexagonais descritas em CLAUDE.md 6.1. Falhar aqui significa que um PR
// introduziu um acoplamento que a revisão manual poderia deixar passar.
class ArquiteturaTest {

  private static final String PACOTE_RAIZ = "br.com.rpe.cartao";

  private static JavaClasses classes;

  @BeforeAll
  static void importarClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PACOTE_RAIZ);
  }

  @Test
  void domainNaoDependeDeFrameworksOuDeOutrasCamadas() {
    noClasses()
        .that()
        .resideInAPackage(PACOTE_RAIZ + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            PACOTE_RAIZ + ".application..",
            PACOTE_RAIZ + ".adapters..",
            PACOTE_RAIZ + ".config..",
            "org.springframework..",
            "jakarta..",
            "org.hibernate..",
            "com.fasterxml.jackson..")
        .because(
            "domain deve ser Java puro, sem Spring/JPA/Jackson e sem depender de outras camadas")
        .check(classes);
  }

  @Test
  void applicationNaoDependeDeAdapters() {
    noClasses()
        .that()
        .resideInAPackage(PACOTE_RAIZ + ".application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(PACOTE_RAIZ + ".adapters..")
        .because(
            "casos de uso não podem depender de controllers, DTOs web, entidades JPA ou clientes HTTP")
        .check(classes);
  }

  @Test
  void adaptersDeEntradaNaoDependemDeAdaptersDeSaida() {
    noClasses()
        .that()
        .resideInAPackage(PACOTE_RAIZ + ".adapters.in..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(PACOTE_RAIZ + ".adapters.out..")
        .because("adapters não se chamam entre si; a comunicação passa pela application")
        .check(classes);
  }

  @Test
  void adaptersDeSaidaNaoDependemDeAdaptersDeEntrada() {
    noClasses()
        .that()
        .resideInAPackage(PACOTE_RAIZ + ".adapters.out..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(PACOTE_RAIZ + ".adapters.in..")
        .because("adapters não se chamam entre si; a comunicação passa pela application")
        .check(classes);
  }

  @Test
  void classesDeDominioSaoLivresDeAnotacoesJpa() {
    classes()
        .that()
        .resideInAPackage(PACOTE_RAIZ + ".domain..")
        .should()
        .notBeAnnotatedWith("jakarta.persistence.Entity")
        .because("entidades JPA vivem em adapters.out.persistence, nunca no domain")
        .check(classes);
  }
}
