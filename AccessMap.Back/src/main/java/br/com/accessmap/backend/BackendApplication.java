package br.com.accessmap.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

// VIA_DTO: serializa Page<T> como PagedModel (formato JSON estavel: content + page.{size,number,totalElements,totalPages}),
// em vez de expor PageImpl diretamente (sem garantia de estabilidade entre versoes do Spring Data).
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication
public class BackendApplication {

	// Todas as datas (LocalDateTime.now) em UTC, na maquina de quem desenvolve e no servidor (#115).
	// No bloco estatico, e nao no main, para valer tambem quando os testes sobem o contexto.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
