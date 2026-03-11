package com.bukubesarkami.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BukuBesarKami API",
                version = "1.0.0",
                description = "General Ledger (Buku Besar) REST API — mencatat arus keuangan dengan akurasi 100%.",
                contact = @Contact(
                        name = "Zaidan Shori",
                        email = "m.zaidanshori04@gmail.com",
                        url = "https://github.com/zaidnshr1/BukuBesarKita"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Development"),
                @Server(url = "https://api.bukubesarkami.com", description = "Production")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}