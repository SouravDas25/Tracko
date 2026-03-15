package com.trako.dtos;

import java.util.List;

public class NamedSeriesDTO {
    private String name;
    private List<StatsPointDTO> series;

    public NamedSeriesDTO() {
    }

    public NamedSeriesDTO(String name, List<StatsPointDTO> series) {
        this.name = name;
        this.series = series;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StatsPointDTO> getSeries() {
        return series;
    }

    public void setSeries(List<StatsPointDTO> series) {
        this.series = series;
    }
}
