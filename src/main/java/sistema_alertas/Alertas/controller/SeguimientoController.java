package sistema_alertas.Alertas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sistema_alertas.Alertas.model.Estudiante;
import sistema_alertas.Alertas.model.Seguimiento;
import sistema_alertas.Alertas.service.SeguimientoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/seguimientos", produces = "application/json")

public class SeguimientoController {

    @Autowired
    private SeguimientoService seguimientoService;

    @GetMapping("/consulta/{consultaId}")
    public ResponseEntity<Seguimiento> obtenerPorConsulta(@PathVariable Integer consultaId) {
        Optional<Seguimiento> resultado = seguimientoService.obtenerPorConsulta(consultaId);
        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Seguimiento> guardar(@RequestBody Seguimiento seguimiento) {
        Seguimiento nuevo = seguimientoService.guardar(seguimiento);
        return ResponseEntity.ok(nuevo);
    }

    @GetMapping("/estudiantes-con-seguimientos")
    public ResponseEntity<List<Estudiante>> estudiantesConSeguimientos() {
        return ResponseEntity.ok(seguimientoService.obtenerEstudiantesConSeguimientos());
    }
}
