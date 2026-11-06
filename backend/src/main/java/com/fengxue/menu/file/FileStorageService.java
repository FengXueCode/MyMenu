package com.fengxue.menu.file;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储：AI 生成图片、兜底 SVG 封面都落在 data/uploads。
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "svg");
    private static final Pattern SAFE_NAME =
            Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(png|jpg|jpeg|webp|svg)");

    private final Path uploadDir;
    private final long maxImageBytes;

    public FileStorageService(@Value("${app.storage.upload-dir:data/uploads}") String uploadDir,
                              @Value("${app.storage.max-image-bytes:5242880}") long maxImageBytes) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxImageBytes = maxImageBytes;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir, e);
        }
    }

    public String saveImage(byte[] data, String extension) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ResultCode.FILE_INVALID, "图片内容为空");
        }
        if (data.length > maxImageBytes) {
            throw new BusinessException(ResultCode.FILE_INVALID, "图片超过大小限制");
        }
        String ext = normalizeExtension(extension);
        String filename = UUID.randomUUID() + "." + ext;
        try {
            Files.write(uploadDir.resolve(filename), data,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("保存图片失败", e);
        }
        return "/api/files/" + filename;
    }

    public String saveSvg(String svg) {
        return saveImage(svg.getBytes(StandardCharsets.UTF_8), "svg");
    }

    public Path resolveSafe(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new BusinessException(ResultCode.FILE_INVALID);
        }
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            throw new BusinessException(ResultCode.FILE_INVALID);
        }
        return path;
    }

    private String normalizeExtension(String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT).replace(".", "");
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_INVALID, "不支持的图片格式");
        }
        return ext;
    }
}
