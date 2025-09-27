package co.edu.escuelaing.propertiesapi.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDto {

    private String address;
    private BigDecimal price;
    private Double size;
    private String description;
}
