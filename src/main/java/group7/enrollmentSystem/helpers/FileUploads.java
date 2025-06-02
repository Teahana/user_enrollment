package group7.enrollmentSystem.helpers;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileUploads {

    private static final String UPLOAD_DIR = "uploadedFiles";

    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            System.err.println("Empty or null file received.");
            return null;
        }

        try {
            // Clean and extract parts of filename
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String baseName = FilenameUtils.getBaseName(originalFilename); // name without extension
            String extension = FilenameUtils.getExtension(originalFilename); // file extension

            // Create filename as originalName_UUID.extension
            String filename = baseName + "_" + UUID.randomUUID() + "." + extension;

            // Create upload directory if not exists
            Path uploadDir = Paths.get("uploadedFiles");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save the file
            Path destination = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "uploadedFiles/" + filename;

        } catch (IOException e) {
            System.err.println("Failed to store file: " + file.getOriginalFilename());
            e.printStackTrace();
            return null;
        }
    }

    public List<String> saveFiles(MultipartFile[] files) {
        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            System.out.println("Attempting to save: " + file.getOriginalFilename());
            String path = saveFile(file);
            if (path != null) savedPaths.add(path);
            else System.err.println("Failed to save file: " + file.getOriginalFilename());
        }
        return savedPaths;
    }
    public String saveSignature(String base64Image) {
        if (base64Image == null || !base64Image.contains("base64,")) {
            throw new IllegalArgumentException("Invalid base64 image format.");
        }

        try {
            String base64Data = base64Image.split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String filename = UUID.randomUUID() + "_signature.png";

            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            Path signaturePath = uploadPath.resolve(filename);
            Files.write(signaturePath, imageBytes);

            return UPLOAD_DIR + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store signature image", e);
        }
    }
    // Function to retrieve a single file
    public byte[] retrieveFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] fileData = new byte[fileBytes.length];
        for (int i = 0; i < fileBytes.length; i++) {
            fileData[i] = fileBytes[i];
        }
        return fileData;
    }

    // Function to retrieve multiple files
    public List<byte[]> retrieveFiles(List<String> filePaths) throws IOException {
        List<byte[]> filesData = new ArrayList<>();
        for (String filePath : filePaths) {
            filesData.add(retrieveFile(filePath));
        }
        return filesData;
    }

    public List<Map<String, String>> getFileMetadata(List<String> filePaths) throws IOException {
        List<Map<String, String>> metadataList = new ArrayList<>();

        for (String path : filePaths) {
            String mimeType = Files.probeContentType(Paths.get(path));
            String name = Paths.get(path).getFileName().toString();

            Map<String, String> info = new HashMap<>();
            info.put("name", name);
            info.put("mimeType", mimeType);
            info.put("path", path);

            metadataList.add(info);
        }

        return metadataList;
    }
    public byte[] createZipFromFiles(List<String> filePaths) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (String path : filePaths) {
                byte[] fileData = retrieveFile(path);
                if (fileData == null) continue;

                String fileName = Paths.get(path).getFileName().toString();
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(fileData);
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        }
    }

}

