package com.jtrull.alzdetection;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
public class AlzDetectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlzDetectionApplication.class, args);
	}

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration corsConf = new CorsConfiguration();
		corsConf.setAllowCredentials(true);
		corsConf.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
		corsConf.setAllowedHeaders(Arrays.asList("Origin", "Access-Control-Allow-Origin", "Content-Type", "Accept", "Authorization", "Origin, Accept", "X-Requested-With",
		"Access-Control-Request-Method", "Access-Control-Request-Headers"));
		corsConf.setExposedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
		corsConf.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
		UrlBasedCorsConfigurationSource url = new UrlBasedCorsConfigurationSource();
		url.registerCorsConfiguration("/**", corsConf);
		return new CorsFilter(url);
	}
}
