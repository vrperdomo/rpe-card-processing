package br.com.rpe.cartao.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PanCriptografiaProperties.class)
public class PanCriptografiaConfig {}
