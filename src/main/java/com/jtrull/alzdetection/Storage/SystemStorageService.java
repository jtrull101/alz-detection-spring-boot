// package com.jtrull.alzdetection.Storage;

// import java.io.IOException;
// import java.io.InputStream;
// import java.net.MalformedURLException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
// import java.util.stream.Stream;

// import org.springframework.core.io.Resource;
// import org.springframework.core.io.UrlResource;
// import org.springframework.stereotype.Service;
// import org.springframework.util.FileSystemUtils;
// import org.springframework.web.multipart.MultipartFile;

// @Service
// public class SystemStorageService implements StorageService {

//     private final Path root;

//     public SystemStorageService(StorageProperties properties) {
//         // if (properties.getLocation().trim().length() == 0)
//         //     throw new StorageException("File upload location is empty");
//         // this.root = Paths.get(properties.getLocation());
//     }

//     @Override
//     public void init() {
//         // try {
//         //     Files.createDirectories(root);
//         // } catch (IOException e) {
//         //     throw new StorageException("Could not initialize storage", e);
//         // }
//     }

//     @Override
//     public void store(MultipartFile file) {
//         // try {
//         //     if (file.isEmpty()) {
//         //         throw new StorageException("Failed to store empty file.");
//         //     }
//         //     Path destinationFile = this.root.resolve(Paths.get(file.getOriginalFilename())).normalize()
//         //             .toAbsolutePath();
//         //     if (!destinationFile.getParent().equals(this.root.toAbsolutePath())) {
//         //         // This is a security check
//         //         throw new StorageException(
//         //                 "Cannot store file outside current directory.");
//         //     }
//         //     try (InputStream inputStream = file.getInputStream()) {
//         //         Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
//         //     }
//         // } catch (IOException e) {
//         //     throw new StorageException("Failed to store file.", e);
//         // }
//     }

//     @Override
//     public Stream<Path> loadAll() {
//         // try {
//         //     return Files.walk(this.root, 1)
//         //             .filter(path -> !path.equals(this.root))
//         //             .map(this.root::relativize);
//         // } catch (IOException e) {
//         //     throw new StorageException("Failed to read stored files", e);
//         // }
//     }

//     @Override
//     public Path load(String filename) {
//         return root.resolve(filename);
//     }

//     @Override
//     public Resource loadAsResource(String filename) {
//         try {
//             Path file = load(filename);
//             Resource resource = new UrlResource(file.toUri());
//             if (resource.exists() || resource.isReadable()) {
//                 return resource;
//             } else {
//                 throw new StorageFileNotFoundException(
//                         "Could not read file: " + filename);

//             }
//         } catch (MalformedURLException e) {
//             throw new StorageFileNotFoundException("Could not read file: " + filename, e);
//         }
//     }

//     @Override
//     public void deleteAll() {
//         FileSystemUtils.deleteRecursively(root.toFile());
//     }

// }
