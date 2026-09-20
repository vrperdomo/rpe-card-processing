package br.com.rpe.produto.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MensageriaProperties.class)
public class MensageriaConfig {}
