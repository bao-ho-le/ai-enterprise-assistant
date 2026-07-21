package com.enterprise.aiassistant.backend;

import com.enterprise.aiassistant.backend.ai.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.ai.vectorstore.config.QdrantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties({QdrantProperties.class, GeminiProperties.class})
public class BackendApplication {

	public static void main(String[] args) {

		SpringApplication.run(BackendApplication.class, args);
	}

}
