package sistema_alertas.Alertas.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sistema_alertas.Alertas.dto.ConsultaResumenDTO;
import sistema_alertas.Alertas.model.Consulta;
import sistema_alertas.Alertas.model.enums.ConsEstado;
import sistema_alertas.Alertas.model.ServicioSms;
import sistema_alertas.Alertas.service.ConsultaService;
import sistema_alertas.Alertas.repository.EstudianteRepository;

@RestController
@RequestMapping(value = "/api/consultas", produces = "application/json")

public class ConsultaController {
    @Autowired
private ServicioSms servicioSms;

    @Autowired
    private ConsultaService consultaService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @GetMapping
    public List<ConsultaResumenDTO> obtenerTodas() {
        return consultaService.obtenerTodas().stream().map(consulta -> {
            var estudiante = consulta.getEstudiante();
            var docente = consulta.getDocente();
            return new ConsultaResumenDTO(
                    consulta.getId(),
                    estudiante.getId(),
                    docente != null ? docente.getId() : null,
                    estudiante.getNombres() + " " + estudiante.getApellidos(),
                    estudiante.getCurso(),
                    estudiante.getImagen(),
                    consulta.getMotivo(),
                    consulta.getFecha(),
                    consulta.getEstado() != null ? consulta.getEstado().name() : null,
                    consulta.getNivel() != null ? consulta.getNivel().name() : null);
        }).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Consulta> obtenerPorId(@PathVariable Integer id) {
        Consulta consulta = consultaService.obtenerPorId(id);
        return consulta != null ? ResponseEntity.ok(consulta) : ResponseEntity.notFound().build();
    }

    @GetMapping("/buscar")
    public List<Consulta> buscarPorMotivo(@RequestParam(required = false) String motivo,
            @RequestParam(required = false) Integer estudianteId) {
        if (motivo != null)
            return consultaService.buscarPorMotivo(motivo);
        if (estudianteId != null)
            return consultaService.buscarPorEstudiante(estudianteId);
        return consultaService.obtenerTodas();
    }

    @PostMapping
    public ResponseEntity<Consulta> guardar(@RequestBody Consulta consulta) {
        Consulta guardada = consultaService.guardar(consulta);

        if (guardada != null && guardada.getEstudiante() != null) {
            enviarSmsAlEstudiante(guardada);
        }

        return ResponseEntity.ok(guardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Consulta> actualizar(@PathVariable Integer id, @RequestBody Consulta nuevaConsulta) {
        Consulta existente = consultaService.obtenerPorId(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if (existente.getEstado() != null && !existente.getEstado().equals(ConsEstado.pendiente)) {
            return ResponseEntity.badRequest().body(null);
        }

        Consulta actualizada = consultaService.actualizar(id, nuevaConsulta);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        Consulta existente = consultaService.obtenerPorId(id);
        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if (existente.getEstado() != null && !existente.getEstado().equals(ConsEstado.pendiente)) {
            return ResponseEntity.badRequest().body("Solo se puede eliminar una alerta en estado pendiente.");
        }

        boolean eliminado = consultaService.eliminar(id);
        return eliminado ? ResponseEntity.ok().build() : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/estudiante/{id}")
    public List<Consulta> obtenerPorEstudiante(@PathVariable Integer id) {
        return consultaService.buscarPorEstudiante(id);
    }

    @GetMapping("/total")
    public long totalConsultas() {
        return consultaService.contar();
    }

    @GetMapping("/estudiantes-con-consultas")
    public ResponseEntity<Long> contarEstudiantesConConsultas() {
        return ResponseEntity.ok(consultaService.contarEstudiantesConConsultas());
    }

    @GetMapping("/sin-seguimiento")
    public List<ConsultaResumenDTO> obtenerConsultasSinSeguimiento() {
        return consultaService.obtenerConsultasSinSeguimiento();
    }

    @PutMapping("/{id}/estado")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> actualizarEstado(@PathVariable Integer id, @RequestBody String nuevoEstado) {
        try {
            ConsEstado estado = ConsEstado.valueOf(nuevoEstado.replace("\"", ""));
            Consulta consulta = consultaService.obtenerPorId(id);
            if (consulta == null)
                return ResponseEntity.notFound().build();
            consulta.setEstado(estado);
            consultaService.guardar(consulta);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado inválido: " + nuevoEstado);
        }
    }

    private void enviarSmsAlEstudiante(Consulta consulta) {
        try {
            Integer estudianteId = consulta.getEstudiante().getId();
            String telefono = estudianteRepository.obtenerSmsPorId(estudianteId);
            String nombreCompleto = estudianteRepository.obtenerNombreCompletoPorId(estudianteId);

            String motivo = consulta.getMotivo();
            String nivel = consulta.getNivel() != null ? consulta.getNivel().name() : "sin definir";

            if (telefono != null && !telefono.trim().isEmpty()) {
                String mensaje = "Se ha registrado una alerta para " + nombreCompleto +
                        ". Motivo: " + motivo + ". Nivel: " + nivel + ".";

               String resultado = servicioSms.enviarMensaje(telefono, mensaje);
                System.out.println(" Resultado envío SMS: " + resultado);
            } else {
                System.out.println(" Número de teléfono no disponible para el estudiante.");
            }

        } catch (Exception e) {
            System.err.println(" Error al enviar SMS: " + e.getMessage());
        }
    }

}
