package com.enterprise.aiassistant.backend.folder.init;

import com.enterprise.aiassistant.backend.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Đảm bảo hệ thống luôn có đúng 1 thư mục gốc ngay từ lần khởi chạy đầu tiên.
@Component
@RequiredArgsConstructor
public class RootFolderInitializer implements ApplicationRunner {

    private final FolderService folderService;

    @Override
    public void run(ApplicationArguments args) {
        folderService.initializeRootFolderIfNotExists();
    }
}
