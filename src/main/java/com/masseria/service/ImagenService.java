package com.masseria.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

@Service
public class ImagenService {

    private static final String UPLOAD_DIR = "src/main/resources/static/img"; 
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> FORMATOS_PERMITIDOS = Arrays.asList("jpg", "jpeg", "png");

    public String guardarImagen(MultipartFile archivo, String nombreCategoria, String nombreProducto) throws IOException {
        validarArchivo(archivo);

        String carpetaCategoria = normalizarNombre(nombreCategoria);
        
        // Obtener la ruta absoluta del directorio de trabajo
        String directorioTrabajo = System.getProperty("user.dir");
        Path directorioUploads = Paths.get(directorioTrabajo, UPLOAD_DIR, carpetaCategoria);
        
        System.out.println("=== DIAGNÓSTICO DE RUTAS ===");
        System.out.println("Directorio de trabajo: " + directorioTrabajo);
        System.out.println("Ruta completa donde se guardará: " + directorioUploads.toString());
        System.out.println("=============================");

        // Crear directorios si no existen
        if (!Files.exists(directorioUploads)) {
            Files.createDirectories(directorioUploads);
            System.out.println("Directorio creado: " + directorioUploads.toString());
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreArchivo = normalizarNombre(nombreProducto) + "." + extension;

        // Ruta física donde se guardará el archivo
        Path destino = directorioUploads.resolve(nombreArchivo);
        
        // Guardar el archivo físicamente
        archivo.transferTo(destino.toFile());
        
        System.out.println("=== IMAGEN GUARDADA FÍSICAMENTE ===");
        System.out.println("Ruta física: " + destino.toAbsolutePath().toString());
        System.out.println("Ruta web: /img/" + carpetaCategoria + "/" + nombreArchivo);
        System.out.println("Archivo existe?: " + Files.exists(destino));
        System.out.println("===================================");

        // Retornar la ruta web para guardar en la base de datos (desde /img/)
        return "/img/" + carpetaCategoria + "/" + nombreArchivo;
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }

        if (archivo.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("La imagen no debe superar los 5MB");
        }

        String extension = obtenerExtension(archivo.getOriginalFilename());
        if (!FORMATOS_PERMITIDOS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                "Formato no permitido. Solo se aceptan: JPG, JPEG, PNG"
            );
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new IllegalArgumentException("El archivo no tiene extensión válida");
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }

    public String normalizarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "sin-nombre";
        }

        // Quitar tildes y diacríticos
        String normalizado = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        // Reemplazar caracteres especiales
        normalizado = normalizado.replaceAll("[ñ]", "n").replaceAll("[Ñ]", "N");
        
        // Minúsculas, espacios a guiones, solo alfanuméricos y guiones
        normalizado = normalizado.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        return normalizado.isEmpty() ? "sin-nombre" : normalizado;
    }
}