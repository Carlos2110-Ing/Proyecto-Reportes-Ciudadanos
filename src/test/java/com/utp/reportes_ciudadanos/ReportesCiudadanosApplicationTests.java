package com.utp.reportes_ciudadanos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReportesCiudadanosApplicationTests {

	@Test
    void contextLoads() {
        // Verifica que Spring Boot + JPA + H2 arranquen sin errores
        assertTrue(true);
    }

	@Test
    void validarGeneracionIniciales() {
        String nombre = "Carlos Castillo";
        String[] partes = nombre.split(" ");
        String iniciales = ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
        
        assertEquals("CC", iniciales);
    }

    @Test
    void validarExtraccionUsername() {
        String email = "usuario@safeperu.pe";
        String username = email.split("@")[0];
        
        assertEquals("usuario", username);
    }

}
