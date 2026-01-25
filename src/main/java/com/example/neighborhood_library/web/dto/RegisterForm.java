package com.example.neighborhood_library.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "Imię jest wymagane.")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane.")
    private String lastName;

    @NotBlank(message = "Login jest wymagany.")
    private String login;

    @NotBlank(message = "Hasło jest wymagane.")
    @Size(min = 8, message = "Hasło musi mieć minimum 8 znaków.")
    private String password;

    @NotBlank(message = "Powtórzenie hasła jest wymagane.")
    private String confirmPassword;

    @NotBlank(message = "Telefon jest wymagany.")
    private String phone;

    @NotBlank(message = "Adres jest wymagany.")
    private String address;

    // getters/setters

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
