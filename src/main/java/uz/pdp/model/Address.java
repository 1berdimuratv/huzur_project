package uz.pdp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Address {

    private Integer id; // u
    private Long userId;
    private String lat;
    private String lon;
    private String body;
    private Boolean viloyat = false;
    private Boolean tuman = false;
    private Boolean dom = false;
    private Boolean xonadon = false;

}
