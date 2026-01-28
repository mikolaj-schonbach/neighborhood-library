package com.example.neighborhood_library.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordForm {

    @NotBlank(message = "Podaj stare hasło.")
    private String oldPassword;

    @NotBlank(message = "Podaj nowe hasło.")
    @Size(min = 8, message = "Nowe hasło musi mieć min. 8 znaków.")
    private String newPassword;

    @NotBlank(message = "Powtórz nowe hasło.")
    private String confirmNewPassword;

    // getters/setters
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmNewPassword() { return confirmNewPassword; }
    public void setConfirmNewPassword(String confirmNewPassword) { this.confirmNewPassword = confirmNewPassword; }
}