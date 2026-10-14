package com.techmart.core.handler.storage;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.command.storage.DeleteFileCommand;
import com.techmart.core.bus.command.storage.UploadFileCommand;
import com.techmart.core.dto.response.storage.FileUploadResponse;
import com.techmart.core.service.storage.StorageService;
import org.springframework.stereotype.Component;

public class StorageHandler {

    @Component
    public static class UploadFileCommandHandler implements CommandHandler<UploadFileCommand, FileUploadResponse> {
        private final StorageService storageService;

        public UploadFileCommandHandler(StorageService storageService) {
            this.storageService = storageService;
        }

        @Override
        public FileUploadResponse handle(UploadFileCommand command) {
            return storageService.uploadFile(command.file(), command.bucket());
        }
    }

    @Component
    public static class DeleteFileCommandHandler implements CommandHandler<DeleteFileCommand, Void> {
        private final StorageService storageService;

        public DeleteFileCommandHandler(StorageService storageService) {
            this.storageService = storageService;
        }

        @Override
        public Void handle(DeleteFileCommand command) {
            storageService.deleteFile(command.fileName(), command.bucket());
            return null;
        }
    }
}
