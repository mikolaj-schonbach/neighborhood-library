package com.example.neighborhood_library.support;

import com.example.neighborhood_library.domain.Loan;
import com.example.neighborhood_library.domain.Message;
import com.example.neighborhood_library.domain.MessageType;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class LoanNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(LoanNotificationJob.class);

    private final LoanRepository loanRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public LoanNotificationJob(LoanRepository loanRepository, MessageRepository messageRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    // Uruchamiaj raz dziennie, np. o 8:00 rano.
    // W środowisku dev można ustawić częściej dla testów, np. co minutę,
    // ale logika opiera się na konkretnych datach (equals), więc wyśle tylko raz dziennie dla danej daty.
    @Scheduled(cron = "${app.notifications.cron:0 0 8 * * *}")
    @Transactional
    public void sendNotifications() {
        LocalDate today = LocalDate.now(clock);

        // 1. Przypomnienie 3 dni przed terminem
        // Szukamy wypożyczeń, które mają due_date = today + 3
        LocalDate warningDate = today.plusDays(3);
        List<Loan> dueSoonLoans = loanRepository.findByDueDateAndReturnedAtIsNull(warningDate);

        for (Loan loan : dueSoonLoans) {
            createMessage(loan, MessageType.DUE_SOON,
                    "Zbliża się termin zwrotu",
                    "Przypominamy, że za 3 dni (" + loan.getDueDate() + ") mija termin zwrotu pozycji: " +
                            loan.getCopy().getPublication().getTitle() + ".");
        }

        // 2. Powiadomienie o przeterminowaniu (np. 1 dzień po terminie)
        // Szukamy wypożyczeń, które miały due_date = yesterday
        LocalDate overdueDate = today.minusDays(1);
        List<Loan> overdueLoans = loanRepository.findByDueDateAndReturnedAtIsNull(overdueDate);

        for (Loan loan : overdueLoans) {
            createMessage(loan, MessageType.OVERDUE,
                    "Termin zwrotu minął!",
                    "Minął termin zwrotu pozycji: " + loan.getCopy().getPublication().getTitle() +
                            ". Prosimy o niezwłoczny zwrot.");
        }

        log.info("Notification job finished. DueSoon: {}, Overdue: {}", dueSoonLoans.size(), overdueLoans.size());
    }

    private void createMessage(Loan loan, MessageType type, String title, String body) {
        Message msg = new Message();
        msg.setUser(loan.getUser());
        msg.setType(type);
        msg.setTitle(title);
        msg.setBody(body);
        // createdAt ustawi się w @PrePersist
        messageRepository.save(msg);
    }
}
