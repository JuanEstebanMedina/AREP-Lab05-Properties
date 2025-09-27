package co.edu.escuelaing.propertiesapi.controller;

import co.edu.escuelaing.propertiesapi.model.dto.PropertyDto;
import co.edu.escuelaing.propertiesapi.model.entity.Property;
import co.edu.escuelaing.propertiesapi.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    @GetMapping
    public Page<Property> list(
            @RequestParam(required = false) String address,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10) Pageable pageable) {
        return service.search(address, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    public Property get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Property create(@Valid @RequestBody PropertyDto p) {
        return service.create(p);
    }

    @PutMapping("/{id}")
    public Property update(@PathVariable Long id, @Valid @RequestBody PropertyDto p) {
        return service.update(id, p);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
