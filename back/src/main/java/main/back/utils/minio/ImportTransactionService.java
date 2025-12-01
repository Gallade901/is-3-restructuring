package main.back.utils.minio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import main.back.person.model.ImportHistory;
import main.back.utils.minio.MinioService;

import java.io.InputStream;

@ApplicationScoped
public class ImportTransactionService {

    @Inject
    private MinioService minioService;

    public boolean executeImportTransaction(EntityManager em,
                                            InputStream fileStream,
                                            String fileName,
                                            String contentType,
                                            long fileSize,
                                            ImportHistory importHistory,
                                            Runnable databaseImportOperation) {

        EntityTransaction transaction = em.getTransaction();
        String storedFileName = null;

        try {
            // Фаза 1: Подготовка - сохраняем файл в MinIO
            transaction.begin();
            em.persist(importHistory);
            em.flush(); // Получаем ID для истории импорта

            storedFileName = generateFileName(importHistory.getId(), fileName);
            minioService.uploadFile(fileStream, storedFileName, contentType, fileSize);

            importHistory.setFileName(storedFileName);
            importHistory.setFileStored(true);
            em.merge(importHistory);

            // Пока не коммитим - оставляем транзакцию открытой

            // Фаза 2: Выполняем основную операцию импорта
            databaseImportOperation.run();

            // Фаза 3: Коммит обеих операций
            transaction.commit();
            return true;

        } catch (Exception e) {
            // Фаза отката
            if (transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rollbackEx) {
                    System.err.println("Error during transaction rollback: " + rollbackEx.getMessage());
                }
            }

            // Компенсирующее действие - удаляем файл из MinIO
            if (storedFileName != null) {
                try {
                    minioService.deleteFile(storedFileName);
                } catch (Exception deleteEx) {
                    System.err.println("Failed to delete file during rollback: " + deleteEx.getMessage());
                }
            }

            // Сохраняем информацию об ошибке
            try {
                EntityTransaction errorTransaction = em.getTransaction();
                errorTransaction.begin();
                ImportHistory errorHistory = em.find(ImportHistory.class, importHistory.getId());
                if (errorHistory != null) {
                    errorHistory.setStatus("ERROR");
                    errorHistory.setFileStored(false);
                    em.merge(errorHistory);
                }
                errorTransaction.commit();
            } catch (Exception ex) {
                System.err.println("Failed to update error status: " + ex.getMessage());
            }

            e.printStackTrace();
            return false;
        }
    }

    private String generateFileName(Long importHistoryId, String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "import_" + importHistoryId + "_" + System.currentTimeMillis() + extension;
    }
}