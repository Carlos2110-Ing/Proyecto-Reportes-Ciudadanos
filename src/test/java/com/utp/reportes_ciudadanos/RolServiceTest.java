package com.utp.reportes_ciudadanos;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
// Ajusta estos imports a tus paquetes de model/repository
import com.utp.reportes_ciudadanos.model.Permiso;
import com.utp.reportes_ciudadanos.model.Rol;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.PermisoJpaRepository;
import com.utp.reportes_ciudadanos.repository.RolJpaRepository;
import com.utp.reportes_ciudadanos.service.RolService;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolJpaRepository rolJpa;

    @Mock
    private PermisoJpaRepository permisoJpa;

    @Mock
    private JsonRepository jsonRepo;

    @InjectMocks
    private RolService rolService;

    private Rol rolEjemplo;
    private Permiso permisoEjemplo;

    @BeforeEach
    void setUp() {
        rolEjemplo = new Rol("USUARIO", "Ciudadano registrado");
        rolEjemplo.setId(1);
        rolEjemplo.setPermisoIds(List.of(10, 20));

        permisoEjemplo = new Permiso("/dashboard", "Ver dashboard", "*");
        permisoEjemplo.setId(10);
    }

    @Test
    void crearRol_DeberiaFormatearNombreEnMayusculasYGuardarEnMySQLYJson() throws IOException {
        when(rolJpa.save(any(Rol.class))).thenAnswer(invocation -> {
            Rol r = invocation.getArgument(0);
            r.setId(5);
            return r;
        });

        Rol resultado = rolService.crearRol("  moderador  ", "Rol de moderación");

        assertNotNull(resultado);
        assertEquals(5, resultado.getId());
        assertEquals("MODERADOR", resultado.getNombre());
        assertEquals("Rol de moderación", resultado.getDescripcion());

        verify(rolJpa, times(1)).save(any(Rol.class));
        verify(jsonRepo, times(1)).saveRol(any(Rol.class));
    }

    @Test
    void findAllRoles_CuandoMySQLFalla_DeberiaActivarFailoverYUsarJson() {
        when(rolJpa.findAll()).thenThrow(new RuntimeException("MySQL caído"));
        when(jsonRepo.findAllRoles()).thenReturn(List.of(rolEjemplo));

        List<Rol> roles = rolService.findAllRoles();

        assertEquals(1, roles.size());
        assertEquals("USUARIO", roles.get(0).getNombre());
        verify(jsonRepo, times(1)).findAllRoles();
    }

    @Test
    void asignarPermisos_DeberiaActualizarListaDePermisosYGuardar() throws IOException {
        when(rolJpa.findById(1)).thenReturn(Optional.of(rolEjemplo));

        List<Integer> nuevosPermisos = List.of(1, 2, 3);
        rolService.asignarPermisos(1, nuevosPermisos);

        assertEquals(nuevosPermisos, rolEjemplo.getPermisoIds());
        verify(rolJpa, times(1)).save(rolEjemplo);
        verify(jsonRepo, times(1)).saveRol(rolEjemplo);
    }

    @Test
    void eliminarRol_CuandoExisteEnMySQLYJson_DeberiaEliminarDeAmbosYRetornarTrue() throws IOException {
        when(rolJpa.existsById(1)).thenReturn(true);
        when(jsonRepo.deleteRol(1)).thenReturn(true);

        boolean eliminado = rolService.eliminarRol(1);

        assertTrue(eliminado);
        verify(rolJpa, times(1)).deleteById(1);
        verify(jsonRepo, times(1)).deleteRol(1);
    }

    @Test
    void crearPermiso_DeberiaGuardarEnMySQLYJson() throws IOException {
        when(permisoJpa.save(any(Permiso.class))).thenAnswer(invocation -> {
            Permiso p = invocation.getArgument(0);
            p.setId(15);
            return p;
        });

        Permiso resultado = rolService.crearPermiso("/reportes/nuevo", "Crear reporte", "POST");

        assertNotNull(resultado);
        assertEquals(15, resultado.getId());
        assertEquals("/reportes/nuevo", resultado.getUrlPattern());
        verify(permisoJpa, times(1)).save(any(Permiso.class));
        verify(jsonRepo, times(1)).savePermiso(any(Permiso.class));
    }

    @Test
    void existePermiso_ConMetodoNulo_DeberiaBuscarConComodinAsterisco() {
        when(permisoJpa.existsByUrlPatternIgnoreCaseAndHttpMethodIgnoreCase("/perfil", "*"))
                .thenReturn(true);

        boolean existe = rolService.existePermiso("  /perfil  ", null);

        assertTrue(existe);
        verify(permisoJpa, times(1))
                .existsByUrlPatternIgnoreCaseAndHttpMethodIgnoreCase("/perfil", "*");
    }

    @Test
    void findPermisosByRolNombre_DeberiaResolverPermisosAsociadosAlRol() {
        Permiso permiso20 = new Permiso("/perfil", "Ver perfil", "*");
        permiso20.setId(20);

        when(rolJpa.findByNombre("USUARIO")).thenReturn(Optional.of(rolEjemplo));
        when(permisoJpa.findById(10)).thenReturn(Optional.of(permisoEjemplo));
        when(permisoJpa.findById(20)).thenReturn(Optional.of(permiso20));

        List<Permiso> permisos = rolService.findPermisosByRolNombre("USUARIO");

        assertEquals(2, permisos.size());
        assertEquals("/dashboard", permisos.get(0).getUrlPattern());
        assertEquals("/perfil", permisos.get(1).getUrlPattern());
    }
}
