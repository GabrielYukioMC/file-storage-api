package com.example.filestorageapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/api/files")
public class FileStorageApiController {

    private final Path fileStorageLocation;
    private final Cloudinary cloudinary;

    public FileStorageApiController(FileStorageApiProperties fileStorageApiProperties) {
        this.fileStorageLocation = Paths.get(fileStorageApiProperties.getUploadDir())
                .toAbsolutePath().normalize();

                this.cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "a",
            "api_key", "a",
            "api_secret", "a",
            "secure", true
        ));
    }

   @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

            String fileDownloadUri = (String) uploadResult.get("url");

            return ResponseEntity.ok().body("Upload completo!! link para Download : " + fileDownloadUri);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("não foi possivel fazer o upload do arquivo");
        }

    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileName") String fileName,
            HttpServletRequest request) {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }

    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() throws IOException {

      List<String> fileName = Files.list(fileStorageLocation)
      .map(Path::getFileName)
        .map(Path::toString)
        .collect(Collectors.toList());

        return ResponseEntity.ok(fileName);
    }

}
