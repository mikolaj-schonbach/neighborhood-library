package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.LibraryInfo;
import com.example.neighborhood_library.repo.LibraryInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryInfoService {

    private final LibraryInfoRepository repository;

    public LibraryInfoService(LibraryInfoRepository repository) {
        this.repository = repository;
    }

    public LibraryInfo getInfo() {
        // Pobieramy ID 1, a jeśli nie ma (pusta baza), zwracamy domyślny placeholder
        return repository.findById(1L).orElseGet(() -> {
            LibraryInfo info = new LibraryInfo();
            info.setAddress("Brak danych");
            info.setOpeningHours("Brak danych");
            info.setRules("Brak danych");
            return info;
        });
    }

    @Transactional
    public void updateInfo(String address, String openingHours, String rules) {
        LibraryInfo info = repository.findById(1L).orElse(new LibraryInfo());
        info.setId((short) 1); // Upewniamy się, że to ID 1
        info.setAddress(address);
        info.setOpeningHours(openingHours);
        info.setRules(rules);
        repository.save(info);
    }
}
