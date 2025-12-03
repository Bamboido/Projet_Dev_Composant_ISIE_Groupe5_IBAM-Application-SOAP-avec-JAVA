package com.example.soap.watcher;

import com.example.soap.cache.ClientCacheService;
import com.example.soap.model.Client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class FileWatcherService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);
    
    @Value("${nifi.output.directory:C:/nifi-1/output}")
    private String nifiOutputDirectory;
    
    @Value("${nifi.file.pattern:clients_*.json}")
    private String filePattern;
    
    @Autowired
    private ClientCacheService cacheService;
    
    private final ObjectMapper objectMapper;
    private File lastProcessedFile;
    
    public FileWatcherService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Initialisation : charge le fichier le plus récent au démarrage
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing FileWatcherService");
        logger.info("Monitoring directory: {}", nifiOutputDirectory);
        logger.info("File pattern: {}", filePattern);
        
        // Créer le répertoire s'il n'existe pas
        File directory = new File(nifiOutputDirectory);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Created monitoring directory: {}", nifiOutputDirectory);
            } else {
                logger.error("Failed to create directory: {}", nifiOutputDirectory);
                return;
            }
        }
        
        // Charger le fichier le plus récent
        loadLatestFile();
    }
    
    /**
     * Vérifie périodiquement s'il y a de nouveaux fichiers (toutes les 10 secondes)
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void checkForNewFiles() {
        logger.debug("Checking for new files in {}", nifiOutputDirectory);
        
        File directory = new File(nifiOutputDirectory);
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist or is not a directory: {}", nifiOutputDirectory);
            return;
        }
        
        File latestFile = getLatestFile(directory);
        
        if (latestFile != null && !latestFile.equals(lastProcessedFile)) {
            logger.info("New file detected: {}", latestFile.getName());
            processFile(latestFile);
            lastProcessedFile = latestFile;
        }
    }
    
    /**
     * Charge le fichier le plus récent au démarrage
     */
    private void loadLatestFile() {
        File directory = new File(nifiOutputDirectory);
        File latestFile = getLatestFile(directory);
        
        if (latestFile != null) {
            logger.info("Loading latest file: {}", latestFile.getName());
            processFile(latestFile);
            lastProcessedFile = latestFile;
        } else {
            logger.warn("No files found in directory: {}", nifiOutputDirectory);
        }
    }
    
    /**
     * Trouve le fichier le plus récent correspondant au pattern
     */
    private File getLatestFile(File directory) {
        File[] files = directory.listFiles((dir, name) -> 
            name.matches(filePattern.replace("*", ".*"))
        );
        
        if (files == null || files.length == 0) {
            return null;
        }
        
        // Trier par date de modification (plus récent en premier)
        return Arrays.stream(files)
            .max(Comparator.comparingLong(File::lastModified))
            .orElse(null);
    }
    
    /**
     * Traite un fichier JSON et met à jour le cache
     */
    private void processFile(File file) {
        try {
            logger.info("Processing file: {}", file.getAbsolutePath());
            
            // Lire le fichier JSON
            List<Client> clients = objectMapper.readValue(file, new TypeReference<List<Client>>() {});
            
            logger.info("Loaded {} clients from file", clients.size());
            
            // Mettre à jour le cache
            cacheService.reloadCache(clients);
            
            logger.info("Cache updated successfully with {} clients", clients.size());
            
        } catch (IOException e) {
            logger.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Force le rechargement manuel du cache
     */
    public void forceReload() {
        logger.info("Force reload requested");
        loadLatestFile();
    }
    
    /**
     * Retourne les informations sur le dernier fichier traité
     */
    public String getLastProcessedFileInfo() {
        if (lastProcessedFile != null) {
            return String.format("File: %s, Size: %d bytes, Last modified: %s",
                lastProcessedFile.getName(),
                lastProcessedFile.length(),
                new java.util.Date(lastProcessedFile.lastModified()));
        }
        return "No file processed yet";
    }
}