package com.utp.reportes_ciudadanos;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
// Ajusta estos imports a tus paquetes de model/repository
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.model.Verificacion;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.UsuarioJpaRepository;
import com.utp.reportes_ciudadanos.repository.VerificacionJpaRepository;
import com.utp.reportes_ciudadanos.service.UsuarioService;


@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioJpaRepository usuarioJpa;

    @Mock
    private VerificacionJpaRepository verificacionJpa;

    @Mock
    private JsonRepository jsonRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1);
        usuarioEjemplo.setNombreCompleto("Carlos Castillo");
        usuarioEjemplo.setEmail("carlos@safeperu.pe");
        usuarioEjemplo.setPassword("$2a$10$hashedPassword");
        usuarioEjemplo.setUsername("carlos");
        usuarioEjemplo.setRol("USUARIO");
        usuarioEjemplo.setAvatarInicial("CC");
    }

    @Test
    void registrar_DeberiaGuardarEnMySQLJsonYCrearVerificacionPendiente() throws IOException {
        // Arrange (Preparar datos simulados)
        when(passwordEncoder.encode("clave123")).thenReturn("$2a$10$encoded");
        when(usuarioJpa.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10); // Simula el ID generado por MySQL
            return u;
        });

        // Act (Ejecutar método real)
        Usuario resultado = usuarioService.registrar("Carlos Castillo", "carlos@safeperu.pe", "clave123");

        // Assert (Verificar resultados)
        assertNotNull(resultado);
        assertEquals(10, resultado.getId());
        assertEquals("carlos@safeperu.pe", resultado.getUsername());
        assertEquals("USUARIO", resultado.getRol());
        assertEquals("CC", resultado.getAvatarInicial());
        assertEquals("$2a$10$encoded", resultado.getPassword());

        // Verificar que se guardó en MySQL, JSON y se creó su verificación
        verify(usuarioJpa, times(1)).save(any(Usuario.class));
        verify(jsonRepo, times(1)).saveUsuario(any(Usuario.class));
        verify(verificacionJpa, times(1)).save(any(Verificacion.class));
        verify(jsonRepo, times(1)).saveVerificacion(any(Verificacion.class));
    }

    @Test
    void findAll_CuandoMySQLFalla_DeberiaActivarFailoverYUsarJson() {
        // Arrange: Simular caída de MySQL
        when(usuarioJpa.findAll()).thenThrow(new RuntimeException("Connection refused"));
        when(jsonRepo.findAllUsuarios()).thenReturn(List.of(usuarioEjemplo));

        // Act
        List<Usuario> lista = usuarioService.findAll();

        // Assert: Debe devolver el resultado del respaldo JSON sin lanzar excepción
        assertEquals(1, lista.size());
        assertEquals("carlos@safeperu.pe", lista.get(0).getEmail());
        verify(jsonRepo, times(1)).findAllUsuarios();
    }

    @Test
    void passwordCorrecta_ConPasswordValida_DeberiaRetornarTrue() {
        when(usuarioJpa.findById(1)).thenReturn(Optional.of(usuarioEjemplo));
        when(passwordEncoder.matches("clave123", "$2a$10$hashedPassword")).thenReturn(true);

        boolean esValida = usuarioService.passwordCorrecta(1, "clave123");

        assertTrue(esValida);
    }

    @Test
    void passwordCorrecta_ConPasswordIncorrecta_DeberiaRetornarFalse() {
        when(usuarioJpa.findById(1)).thenReturn(Optional.of(usuarioEjemplo));
        when(passwordEncoder.matches("incorrecta", "$2a$10$hashedPassword")).thenReturn(false);

        boolean esValida = usuarioService.passwordCorrecta(1, "incorrecta");

        assertFalse(esValida);
    }

    @Test
    void cambiarRol_DeberiaFormatearEnMayusculasYGuardar() throws IOException {
        when(usuarioJpa.findById(1)).thenReturn(Optional.of(usuarioEjemplo));

        usuarioService.cambiarRol(1, "  admin  ");

        assertEquals("ADMIN", usuarioEjemplo.getRol());
        verify(usuarioJpa, times(1)).save(usuarioEjemplo);
        verify(jsonRepo, times(1)).saveUsuario(usuarioEjemplo);
    }

    @Test
    void verificarUsuario_DeberiaCambiarEstadoAVerificadoYAsignarAdmin() throws IOException {
        Verificacion verifPendiente = new Verificacion();
        verifPendiente.setId(5);
        verifPendiente.setUsuarioId(1);
        verifPendiente.setEstado(Verificacion.Estado.PENDIENTE);

        when(verificacionJpa.findByUsuarioId(1)).thenReturn(Optional.of(verifPendiente));

        usuarioService.verificarUsuario(1, 99);

        assertEquals(Verificacion.Estado.VERIFICADO, verifPendiente.getEstado());
        assertEquals(99, verifPendiente.getAdminId());
        assertNotNull(verifPendiente.getFechaVerificacion());
        verify(verificacionJpa, times(1)).save(verifPendiente);
        verify(jsonRepo, times(1)).saveVerificacion(verifPendiente);
    }

    @Test
    void registrar_ConUnSoloNombre_DeberiaGenerarInicialesDeDosLetras() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(usuarioJpa.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = usuarioService.registrar("Carlos", "carlos@safeperu.pe", "1234");

        assertEquals("CA", resultado.getAvatarInicial());
    }
}
