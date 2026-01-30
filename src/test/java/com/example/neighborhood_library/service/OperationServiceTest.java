package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Copy;
import com.example.neighborhood_library.domain.OperationHistory;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.OperationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationHistoryRepository historyRepository;

    @InjectMocks
    private OperationService operationService;

    @Test
    void logAction_ShouldCreateAndSaveOperationHistory() {
        // given
        User actor = new User(); // wykonujący akcję
        User target = new User(); // cel akcji
        String actionName = "TEST_ACTION";
        Copy copy = new Copy(); // powiązany egzemplarz

        // when
        operationService.logAction(actor, target, actionName, copy);

        // then
        ArgumentCaptor<OperationHistory> captor = ArgumentCaptor.forClass(OperationHistory.class);
        verify(historyRepository).save(captor.capture());

        OperationHistory savedEntry = captor.getValue();

        // Weryfikacja poprawności mapowania pól
        assertEquals(actor, savedEntry.getActor());
        assertEquals(target, savedEntry.getTargetUser());

        // POPRAWKA: getAction() zamiast getActionType()
        assertEquals(actionName, savedEntry.getAction());
        assertEquals(copy, savedEntry.getCopy());

        // Timestamp jest ustawiany w @PrePersist (czyli przez JPA),
        // ale w teście jednostkowym z Mockito metoda prePersist się nie wykona automatycznie.
        // Konstruktor OperationHistory nie ustawia happenedAt.
        // Dlatego w teście unitowym to pole będzie nullem tuż po stworzeniu obiektu przez "new".
        assertNull(savedEntry.getHappenedAt(),
                "W teście unitowym happenedAt jest null, bo @PrePersist nie działa poza kontenerem JPA");
    }

    @Test
    void logAction_ShouldHandleNullOptionalFields() {
        // given
        User actor = new User();
        String actionName = "SYSTEM_ACTION";

        // when
        // target i copy mogą być null
        operationService.logAction(actor, null, actionName, null);

        // then
        ArgumentCaptor<OperationHistory> captor = ArgumentCaptor.forClass(OperationHistory.class);
        verify(historyRepository).save(captor.capture());

        OperationHistory savedEntry = captor.getValue();
        assertEquals(actor, savedEntry.getActor());
        assertNull(savedEntry.getTargetUser());


        assertEquals(actionName, savedEntry.getAction());
        assertNull(savedEntry.getCopy());
    }
}