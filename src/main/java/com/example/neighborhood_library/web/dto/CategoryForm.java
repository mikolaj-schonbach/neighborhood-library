package com.example.neighborhood_library.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "Nazwa kategorii jest wymagana.")
    @Size(max = 120, message = "Nazwa kategorii jest za długa.")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
