package com.bukubesarkami;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BukuBesarKamiApplication {
	public static void main(String[] args) {
		SpringApplication.run(BukuBesarKamiApplication.class, args);
	}
}