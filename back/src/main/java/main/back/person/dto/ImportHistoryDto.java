package main.back.person.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ImportHistoryDto {
    private Long id;
    private String userLogin;
    private String status;
    private Integer importedCount;
    private LocalDateTime importDate;
    private String fileName;
    private Boolean fileStored;
    private String fileDownloadUrl;
    public ImportHistoryDto(Long id, String userLogin, String status,
                            Integer importedCount, LocalDateTime importDate,
                            String fileName, Boolean fileStored) {
        this.id = id;
        this.userLogin = userLogin;
        this.status = status;
        this.importedCount = importedCount;
        this.importDate = importDate;
        this.fileName = fileName;
        this.fileStored = fileStored;
    }
}