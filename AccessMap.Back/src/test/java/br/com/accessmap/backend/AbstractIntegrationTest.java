package br.com.accessmap.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Base para testes que sobem o contexto completo contra um Postgres descartável.
 * <p>
 * O container é um singleton da JVM (iniciado no static initializer, sem {@code @Container}):
 * o contexto Spring é cacheado entre classes de teste, e se o container fosse reiniciado por
 * classe o pool de conexões cacheado apontaria para uma porta morta. O Testcontainers encerra
 * o container ao fim da JVM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    private static final AtomicLong SEQUENCIA_TELEFONE = new AtomicLong(System.nanoTime() % 100_000_000L);

    /** Telefone válido (11 dígitos) e único na JVM: o banco é compartilhado entre as classes e o telefone é UNIQUE. */
    protected static String telefoneUnico() {
        return String.format("119%08d", SEQUENCIA_TELEFONE.incrementAndGet() % 100_000_000L);
    }
}
