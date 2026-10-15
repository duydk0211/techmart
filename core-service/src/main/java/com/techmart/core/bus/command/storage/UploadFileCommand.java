package com.techmart.core.bus.command.storage;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.response.storage.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public record UploadFileCommand(MultipartFile file, String bucket) implements Command<FileUploadResponse> {
}
