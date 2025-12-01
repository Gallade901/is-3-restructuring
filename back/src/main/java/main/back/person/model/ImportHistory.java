package main.back.person.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import main.back.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_history")
@Getter
@Setter
public class ImportHistory {
    public ImportHistory() {
    }

    public ImportHistory(User user, String status, int importedCount, LocalDateTime importDate) {
        this.user = user;
        this.status = status;
        this.importedCount = importedCount;
        this.importDate = importDate;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status; // SUCCESS, ERROR

    @Column(name = "imported_count")
    private Integer importedCount;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_stored")
    private Boolean fileStored = false;

    @PrePersist
    protected void onCreate() {
        if (importDate == null) {
            importDate = LocalDateTime.now();
        }
    }
}