package com.example.neighborhood_library.web.dto;

import com.example.neighborhood_library.domain.User;
import jakarta.validation.constraints.NotBlank;

public class EditProfileForm {

    @NotBlank(message = "Imię jest wymagane.")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane.")
    private String lastName;

    @NotBlank(message = "Telefon jest wymagany.")
    private String phone;

    @NotBlank(message = "Adres jest wymagany.")
    private String address;

    public EditProfileForm() {}

    // Konstruktor pomocniczy do wypełnienia formularza danymi z bazy
    public EditProfileForm(User user) {
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.address = user.getAddress();
    }

    // getters & setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
