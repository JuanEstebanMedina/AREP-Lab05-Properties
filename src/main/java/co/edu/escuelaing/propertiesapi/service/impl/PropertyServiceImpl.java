package co.edu.escuelaing.propertiesapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import co.edu.escuelaing.propertiesapi.model.dto.PropertyDto;
import co.edu.escuelaing.propertiesapi.model.entity.Property;
import co.edu.escuelaing.propertiesapi.repository.PropertyRepository;
import co.edu.escuelaing.propertiesapi.service.PropertyService;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository repo;

    @Override
    public Property create(PropertyDto p) {
        Property property = Property.builder()
                .address(p.getAddress())
                .price(p.getPrice())
                .size(p.getSize())
                .description(p.getDescription())
                .build();
        return repo.save(property);
    }

    @Override
    public Page<Property> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public Property get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Property not found"));
    }

    @Override
    public Property update(Long id, PropertyDto p) {
        Property current = get(id);
        current.setAddress(p.getAddress());
        current.setPrice(p.getPrice());
        current.setSize(p.getSize());
        current.setDescription(p.getDescription());
        return repo.save(current);
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NoSuchElementException("Property not found");
        }
        repo.deleteById(id);
    }

    @Override
    public Page<Property> search(String address, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        boolean hasAddress = address != null && !address.isBlank();
        boolean hasMin = minPrice != null;
        boolean hasMax = maxPrice != null;

        if (!hasAddress && !hasMin && !hasMax) {
            return list(pageable);
        }

        Specification<Property> spec = null;

        if (hasAddress) {
            Specification<Property> s = addressContains(address);
            spec = (spec == null) ? s : spec.and(s);
        }

        if (hasMin || hasMax) {
            BigDecimal min = hasMin ? minPrice : BigDecimal.valueOf(0);
            BigDecimal max = hasMax ? maxPrice : new BigDecimal("9999999999");
            if (min.compareTo(max) > 0) {
                BigDecimal tmp = min;
                min = max;
                max = tmp;
            }
            Specification<Property> s = priceBetween(min, max);
            spec = (spec == null) ? s : spec.and(s);
        }

        return repo.findAll(spec, pageable);
    }

    // Specification helpers
    private Specification<Property> addressContains(String address) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase() + "%");
    }

    private Specification<Property> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("price"), min, max);
    }
}