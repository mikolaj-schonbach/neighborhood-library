package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.LibraryInfo;
import com.example.neighborhood_library.repo.LibraryInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryInfoServiceTest {

    @Mock
    private LibraryInfoRepository repository;

    @InjectMocks
    private LibraryInfoService service;

    @Test
    void getInfo_ShouldReturnExistingInfo_WhenPresent() {
        // given
        LibraryInfo info = new LibraryInfo();
        info.setAddress("Existing Address");
        ReflectionTestUtils.setField(info, "id", (short) 1);

        when(repository.findById(1L)).thenReturn(Optional.of(info));

        // when
        LibraryInfo result = service.getInfo();

        // then
        assertEquals("Existing Address", result.getAddress());
        assertEquals((short) 1, result.getId());
    }

    @Test
    void getInfo_ShouldReturnDefaultPlaceholder_WhenMissing() {
        // given
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // when
        LibraryInfo result = service.getInfo();

        // then
        assertEquals("Brak danych", result.getAddress());
        assertEquals("Brak danych", result.getOpeningHours());
        assertEquals("Brak danych", result.getRules());
    }

    @Test
    void updateInfo_ShouldUpdateExistingRecord() {
        // given
        LibraryInfo existing = new LibraryInfo();
        existing.setAddress("Old");
        ReflectionTestUtils.setField(existing, "id", (short) 1);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        // when
        service.updateInfo("New Address", "9-17", "No running");

        // then
        assertEquals("New Address", existing.getAddress());
        assertEquals("9-17", existing.getOpeningHours());
        assertEquals("No running", existing.getRules());
        verify(repository).save(existing);
    }

    @Test
    void updateInfo_ShouldCreateNewRecord_WhenNoneExists() {
        // given
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // when
        service.updateInfo("New Address", "9-17", "No running");

        // then
        ArgumentCaptor<LibraryInfo> captor = ArgumentCaptor.forClass(LibraryInfo.class);
        verify(repository).save(captor.capture());

        LibraryInfo saved = captor.getValue();
        assertEquals((short) 1, saved.getId()); // Musi wymusić ID=1
        assertEquals("New Address", saved.getAddress());
    }
}
