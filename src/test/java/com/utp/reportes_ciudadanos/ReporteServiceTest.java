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
import com.utp.reportes_ciudadanos.model.Categoria;
import com.utp.reportes_ciudadanos.model.Reporte;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.repository.CategoriaJpaRepository;
import com.utp.reportes_ciudadanos.repository.JsonRepository;
import com.utp.reportes_ciudadanos.repository.ReporteJpaRepository;
import com.utp.reportes_ciudadanos.service.ReporteService;
import com.utp.reportes_ciudadanos.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteJpaRepository reporteJpa;

    @Mock
    private CategoriaJpaRepository categoriaJpa;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JsonRepository jsonRepo;

    @InjectMocks
    private ReporteService reporteService;

    private Reporte reporteEjemplo;
    private Categoria categoriaEjemplo;
    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        categoriaEjemplo = new Categoria();
        categoriaEjemplo.setId(1);
        categoriaEjemplo.setNombre("Infraestructura");

        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(10);
        usuarioEjemplo.setNombreCompleto("Carlos Castillo");
        usuarioEjemplo.setAvatarInicial("CC");

        reporteEjemplo = new Reporte();
        reporteEjemplo.setId(100);
        reporteEjemplo.setTitulo("Bache en avenida principal");
        reporteEjemplo.setDescripcion("Hueco peligroso frente al parque");
        reporteEjemplo.setCategoriaId(1);
        reporteEjemplo.setUbicacion("Av. José de Lama, Sullana");
        reporteEjemplo.setLatitud(-4.9039);
        reporteEjemplo.setLongitud(-80.6853);
        reporteEjemplo.setUsuarioId(10);
        reporteEjemplo.setEstado(Reporte.Estado.PENDIENTE);
        reporteEjemplo.setFechaCreacion("2026-11-15T10:00:00");
    }

    @Test
    void crear_DeberiaGuardarReporteConEstadoPendienteEnMySQLYJson() throws IOException {
        when(reporteJpa.save(any(Reporte.class))).thenAnswer(invocation -> {
            Reporte r = invocation.getArgument(0);
            r.setId(100);
            return r;
        });

        Reporte resultado = reporteService.crear(
                "Bache en avenida principal",
                "Hueco peligroso frente al parque",
                1,
                "Av. José de Lama, Sullana",
                -4.9039,
                -80.6853,
                10);

        assertNotNull(resultado);
        assertEquals(100, resultado.getId());
        assertEquals(Reporte.Estado.PENDIENTE, resultado.getEstado());
        assertNotNull(resultado.getFechaCreacion());

        verify(reporteJpa, times(1)).save(any(Reporte.class));
        verify(jsonRepo, times(1)).saveReporte(any(Reporte.class));
    }

    @Test
    void findById_DeberiaEnriquecerReporteConCategoriaYDatosDeUsuario() {
        when(reporteJpa.findById(100)).thenReturn(Optional.of(reporteEjemplo));
        when(categoriaJpa.findById(1)).thenReturn(Optional.of(categoriaEjemplo));
        when(usuarioService.findById(10)).thenReturn(Optional.of(usuarioEjemplo));
        when(usuarioService.isVerificado(10)).thenReturn(true);

        Optional<Reporte> resultado = reporteService.findById(100);

        assertTrue(resultado.isPresent());
        Reporte r = resultado.get();
        assertEquals("Infraestructura", r.getCategoria().getNombre());
        assertEquals("Carlos Castillo", r.getUsuarioNombre());
        assertEquals("CC", r.getUsuarioAvatar());
        assertTrue(r.isUsuarioVerificado());
    }

    @Test
    void findAll_CuandoMySQLFalla_DeberiaActivarFailoverYOrdenarDesdeJson() {
        Reporte reporteMasReciente = new Reporte();
        reporteMasReciente.setId(101);
        reporteMasReciente.setCategoriaId(1);
        reporteMasReciente.setUsuarioId(10);
        reporteMasReciente.setFechaCreacion("2026-11-15T14:30:00");

        when(reporteJpa.findAllByOrderByFechaCreacionDesc())
                .thenThrow(new RuntimeException("MySQL no disponible"));
        when(jsonRepo.findAllReportes())
                .thenReturn(List.of(reporteEjemplo, reporteMasReciente));
        when(categoriaJpa.findById(1)).thenReturn(Optional.of(categoriaEjemplo));
        when(usuarioService.findById(10)).thenReturn(Optional.of(usuarioEjemplo));

        List<Reporte> lista = reporteService.findAll();

        assertEquals(2, lista.size());
        // Debe ordenar descendentemente por fechaCreacion (el 101 primero)
        assertEquals(101, lista.get(0).getId());
        assertEquals(100, lista.get(1).getId());
        verify(jsonRepo, times(1)).findAllReportes();
    }

    @Test
    void cambiarEstado_DeberiaLimpiarCamposTransientYActualizarEstado() throws IOException {
        when(reporteJpa.findById(100)).thenReturn(Optional.of(reporteEjemplo));
        when(categoriaJpa.findById(1)).thenReturn(Optional.of(categoriaEjemplo));
        when(usuarioService.findById(10)).thenReturn(Optional.of(usuarioEjemplo));

        reporteService.cambiarEstado(100, Reporte.Estado.RESUELTO);

        assertEquals(Reporte.Estado.RESUELTO, reporteEjemplo.getEstado());
        // Verifica que guardarReporte limpió los campos @Transient antes de persistir
        assertNull(reporteEjemplo.getCategoria());
        assertNull(reporteEjemplo.getUsuarioNombre());
        assertNull(reporteEjemplo.getUsuarioAvatar());

        verify(reporteJpa, times(1)).save(reporteEjemplo);
        verify(jsonRepo, times(1)).saveReporte(reporteEjemplo);
    }

    @Test
    void eliminarSiEsDueno_CuandoUsuarioEsElAutor_DeberiaEliminarYRetornarTrue() throws IOException {
        when(reporteJpa.findById(100)).thenReturn(Optional.of(reporteEjemplo));
        when(jsonRepo.deleteReporteSiEsDueno(100, 10)).thenReturn(true);

        boolean eliminado = reporteService.eliminarSiEsDueno(100, 10);

        assertTrue(eliminado);
        verify(reporteJpa, times(1)).deleteById(100);
        verify(jsonRepo, times(1)).deleteReporteSiEsDueno(100, 10);
    }

    @Test
    void eliminarSiEsDueno_CuandoUsuarioNoEsElAutor_NoDeberiaEliminarEnMySQL() throws IOException {
        when(reporteJpa.findById(100)).thenReturn(Optional.of(reporteEjemplo));
        when(jsonRepo.deleteReporteSiEsDueno(100, 999)).thenReturn(false);

        boolean eliminado = reporteService.eliminarSiEsDueno(100, 999);

        assertFalse(eliminado);
        verify(reporteJpa, never()).deleteById(any());
    }

    @Test
    void countTotal_CuandoMySQLFalla_DeberiaContarDesdeJson() {
        when(reporteJpa.count()).thenThrow(new RuntimeException("Error BD"));
        when(jsonRepo.findAllReportes()).thenReturn(List.of(reporteEjemplo));

        long total = reporteService.countTotal();

        assertEquals(1L, total);
    }
}
