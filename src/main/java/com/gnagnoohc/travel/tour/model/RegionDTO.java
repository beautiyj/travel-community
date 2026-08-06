package com.gnagnoohc.travel.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionDTO {
    private Integer regionId;
    private String regionName;
    private Integer parentRegionId;
}
