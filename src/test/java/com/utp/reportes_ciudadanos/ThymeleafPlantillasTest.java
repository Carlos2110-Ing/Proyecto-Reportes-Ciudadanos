package com.utp.reportes_ciudadanos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.reportes_ciudadanos.config.DataInitializer;
import com.utp.reportes_ciudadanos.model.Permiso;
import com.utp.reportes_ciudadanos.model.Usuario;
import com.utp.reportes_ciudadanos.service.ReporteService;
import com.utp.reportes_ciudadanos.service.RolService;
import com.utp.reportes_ciudadanos.service.UsuarioService;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class ThymeleafPlantillasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private DataInitializer dataInitializer;

    @MockitoBean
    private ReporteService reporteService;

    @MockitoBean
    private RolService rolService;

    @BeforeEach
    void setUp() {
        prepararServicios();
    }

    @Test
    void renderizaVistasPublicasConLayoutAuthYFragmentosCompartidos() throws Exception {
        assertPage("/", "Bienvenido – SafePeru", "Reporta incidentes", null, false);
        assertPage("/login", "Iniciar Sesión – SafePeru", "Inicia sesión en tu cuenta", null, false);
        assertPage("/registro", "Crear Cuenta – SafePeru", "Crea tu cuenta ciudadana", null, false);
    }

    @Test
    void renderizaVistasDeUsuarioConNavbarFooterYDatosDelPrincipal() throws Exception {
        assertUserPage("/dashboard", "Dashboard", "Mapa de Incidentes", "USUARIO");
        assertUserPage("/reportes/nuevo", "Nuevo Reporte", "Nuevo Reporte Ciudadano", "USUARIO");
        assertUserPage("/reportes/mis-reportes", "Mis Reportes", "Gestiona todos tus reportes ciudadanos", "USUARIO");
        assertUserPage("/perfil", "Mi Perfil", "Datos de Cuenta", "USUARIO");
        assertPage("/acerca-de", "Acerca de – SafePeru", "Acerca de SafePeru", testUser("USUARIO"), true);
    }

    @Test
    void renderizaVistaAdminYRespetaCondicionalesDeRolEnNavbar() throws Exception {
        assertUserPage("/dashboard", "Dashboard", "Mapa de Incidentes", "ADMIN");
        assertAdminPage();
        assertAdminPage("/admin/");
    }

    @Test
    void navbarUserMuestraEnlaceAdminSoloConAutoridadAdmin() throws Exception {
        Usuario usuario = testUser("USUARIO");
        Usuario administrador = testUser("USUARIO", "ADMIN");

        mockMvc.perform(get("/dashboard").with(user(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Ciudadano")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Panel Admin"))));

        mockMvc.perform(get("/dashboard").with(user(administrador)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Ciudadano")))
                .andExpect(content().string(containsString("Panel Admin")));
    }

    private void assertUserPage(String path, String title, String mainContent, String role) throws Exception {
        assertPage(path, title + " – SafePeru", mainContent, testUser(role),
                "/acerca-de".equals(path));
    }

    private void assertPage(String path, String expectedTitle, String mainContent,
            UserDetails principal, boolean needsSession) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (principal != null) {
            request = request.with(user(principal));
        }
        if (needsSession) {
            request.sessionAttr("usuarioId", 1);
        }

        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>" + expectedTitle + "</title>")))
                .andExpect(content().string(containsString(mainContent)))
                .andExpect(content().string(containsString("SafePeru")));

        if (principal != null) {
            result.andExpect(content().string(containsString("Carlos Ciudadano")))
                    .andExpect(content().string(containsString("CC")))
                    .andExpect(content().string(containsString("navbar-safeperu")))
                    .andExpect(content().string(containsString("footer-safeperu")));
        } else {
            result.andExpect(content().string(containsString("navbar-safeperu")))
                    .andExpect(content().string(containsString("footer-safeperu")));
        }
    }

    private void assertAdminPage() throws Exception {
        assertAdminPage("/admin");
    }

    private void assertAdminPage(String path) throws Exception {
        mockMvc.perform(get(path).with(user(testUser("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Panel Admin – SafePeru Admin</title>")))
                .andExpect(content().string(containsString("Gestión de Usuarios y Verificación")))
                .andExpect(content().string(containsString("Panel Administrador")));
    }

    private void prepararServicios() {
        List<Permiso> permisosUsuario = List.of(
                new Permiso("/dashboard", "Ver dashboard", "*"),
                new Permiso("/reportes/nuevo", "Crear reporte", "*"),
                new Permiso("/reportes/mis-reportes", "Ver reportes", "*"),
                new Permiso("/perfil", "Ver perfil", "*"),
                new Permiso("/acerca-de", "Ver acerca de", "*"));
        List<Permiso> permisosAdmin = new java.util.ArrayList<>(permisosUsuario);
        permisosAdmin.add(new Permiso("/admin/**", "Administrar", "*"));
        when(rolService.findPermisosByRolNombre(anyString())).thenAnswer(invocation ->
                "ADMIN".equals(invocation.getArgument(0)) ? permisosAdmin : permisosUsuario);

        when(reporteService.countTotal()).thenReturn(0L);
        when(reporteService.countPendiente()).thenReturn(0L);
        when(reporteService.countEnProceso()).thenReturn(0L);
        when(reporteService.countResuelto()).thenReturn(0L);
        when(reporteService.findUltimos(3)).thenReturn(List.of());
        when(reporteService.findAll()).thenReturn(List.of());
        when(reporteService.findAllCategorias()).thenReturn(List.of());
        when(reporteService.findByUsuario(1)).thenReturn(List.of());
        when(reporteService.countByUsuario(1)).thenReturn(0L);
        when(reporteService.countResueltosByUsuario(1)).thenReturn(0L);

        when(usuarioService.isVerificado(1)).thenReturn(false);
        when(usuarioService.findVerificacionByUsuario(1)).thenReturn(java.util.Optional.empty());
        when(usuarioService.findAll()).thenReturn(List.of());
        when(usuarioService.findAllVerificaciones()).thenReturn(List.of());

        when(rolService.findAllRoles()).thenReturn(List.of());
        when(rolService.findAllPermisos()).thenReturn(List.of());
    }

    private Usuario testUser(String... roles) {
        Usuario usuario = new Usuario() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Arrays.stream(roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
            }
        };
        usuario.setId(1);
        usuario.setUsername("Carlos Ciudadano");
        usuario.setEmail("carlos@safeperu.pe");
        usuario.setPassword("TestPassword123");
        usuario.setNombreCompleto("Carlos Ciudadano");
        usuario.setAvatarInicial("CC");
        usuario.setRol(roles.length == 0 ? "USUARIO" : roles[0]);
        return usuario;
    }

}
