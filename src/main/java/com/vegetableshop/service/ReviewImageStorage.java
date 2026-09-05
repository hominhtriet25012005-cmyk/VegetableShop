package com.vegetableshop.service;

import com.vegetableshop.exception.ReviewOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
@Profile("mysql")
public class ReviewImageStorage {
    private final Path root;
    public ReviewImageStorage(@Value("${app.reviews.image-directory:./uploads/reviews}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public List<String> store(List<MultipartFile> files) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Review images require a transaction");
        }
        if (files == null) return List.of();
        var selected = files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (selected.size() > 5) throw invalid("Chỉ được gửi tối đa 5 ảnh");
        // Validate and re-encode all images before writing any file. Original metadata is discarded.
        List<byte[]> normalized = selected.stream().map(this::normalize).toList();
        List<String> keys = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) keys.forEach(ReviewImageStorage.this::deleteCreatedFile);
            }
        });
        try {
            if (!normalized.isEmpty()) Files.createDirectories(root);
            for (byte[] bytes : normalized) {
                String key = UUID.randomUUID() + ".jpg";
                keys.add(key);
                Files.write(resolve(key), bytes, StandardOpenOption.CREATE_NEW);
            }
            return List.copyOf(keys);
        } catch (IOException exception) {
            keys.forEach(this::deleteCreatedFile);
            throw invalid("Không lưu được ảnh. Vui lòng thử lại sau");
        }
    }

    private byte[] normalize(MultipartFile file) {
        if (file.getSize() > 2 * 1024 * 1024) throw invalid("Mỗi ảnh tối đa 2 MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")))
            throw invalid("Chỉ chấp nhận ảnh JPG hoặc PNG");
        try (var input = new MemoryCacheImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("Tệp tải lên không phải ảnh hợp lệ");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw invalid("Chỉ chấp nhận ảnh JPG hoặc PNG");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 6000 || height > 6000 || (long) width * height > 12_000_000)
                    throw invalid("Ảnh tối đa 12 megapixel và 6000 pixel mỗi chiều");
                BufferedImage original = reader.read(0);
                double scale = Math.min(1.0, 1600.0 / Math.max(width, height));
                BufferedImage clean = new BufferedImage(Math.max(1, (int)(width * scale)),
                    Math.max(1, (int)(height * scale)), BufferedImage.TYPE_INT_RGB);
                var graphics = clean.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, clean.getWidth(), clean.getHeight());
                    graphics.drawImage(original, 0, 0, clean.getWidth(), clean.getHeight(), null);
                } finally { graphics.dispose(); original.flush(); }
                var output = new ByteArrayOutputStream();
                ImageIO.write(clean, "jpg", output);
                clean.flush();
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException exception) { throw invalid("Không đọc được ảnh. Vui lòng chọn ảnh JPG/PNG khác"); }
    }

    public byte[] read(String key) throws IOException { return Files.readAllBytes(resolve(key)); }
    private Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}\\.jpg")) throw invalid("Mã ảnh không hợp lệ");
        Path file = root.resolve(key).normalize();
        if (!file.getParent().equals(root)) throw invalid("Mã ảnh không hợp lệ");
        return file;
    }
    private void deleteCreatedFile(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException exception) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not clean rolled-back review image {}", key);
        }
    }
    private ReviewOperationException invalid(String message) { return new ReviewOperationException(message); }
}
