package com.example.soap.cache;

import com.example.soap.model.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ClientCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientCacheService.class);
    
    // Cache thread-safe des clients
    private final Map<Long, Client> clientCache = new ConcurrentHashMap<>();
    
    // Index par email pour recherche rapide
    private final Map<String, Client> emailIndex = new ConcurrentHashMap<>();
    
    /**
     * Recharge complètement le cache avec une nouvelle liste de clients
     */
    public synchronized void reloadCache(List<Client> clients) {
        logger.info("Reloading client cache with {} clients", clients.size());
        
        // Vider les caches
        clientCache.clear();
        emailIndex.clear();
        
        // Recharger
        for (Client client : clients) {
            if (client.getId() != null) {
                clientCache.put(client.getId(), client);
                if (client.getEmail() != null) {
                    emailIndex.put(client.getEmail().toLowerCase(), client);
                }
            }
        }
        
        logger.info("Cache reloaded successfully. Total clients: {}", clientCache.size());
    }
    
    /**
     * Ajoute ou met à jour un client dans le cache
     */
    public void putClient(Client client) {
        if (client.getId() != null) {
            clientCache.put(client.getId(), client);
            if (client.getEmail() != null) {
                emailIndex.put(client.getEmail().toLowerCase(), client);
            }
            logger.debug("Client added/updated in cache: {}", client.getId());
        }
    }
    
    /**
     * Récupère tous les clients
     */
    public List<Client> getAllClients() {
        return new ArrayList<>(clientCache.values());
    }
    
    /**
     * Récupère un client par ID
     */
    public Client getClientById(Long id) {
        return clientCache.get(id);
    }
    
    /**
     * Récupère un client par email
     */
    public Client getClientByEmail(String email) {
        if (email == null) {
            return null;
        }
        return emailIndex.get(email.toLowerCase());
    }
    
    /**
     * Recherche des clients par critères
     */
    public List<Client> searchClients(String ville, String nom) {
        return clientCache.values().stream()
            .filter(client -> {
                boolean match = true;
                if (ville != null && !ville.isEmpty()) {
                    match = client.getVille() != null && 
                            client.getVille().toLowerCase().contains(ville.toLowerCase());
                }
                if (match && nom != null && !nom.isEmpty()) {
                    match = client.getNom() != null && 
                            client.getNom().toLowerCase().contains(nom.toLowerCase());
                }
                return match;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Supprime un client du cache
     */
    public boolean deleteClient(Long id) {
        Client removed = clientCache.remove(id);
        if (removed != null && removed.getEmail() != null) {
            emailIndex.remove(removed.getEmail().toLowerCase());
            logger.debug("Client removed from cache: {}", id);
            return true;
        }
        return false;
    }
    
    /**
     * Retourne le nombre de clients en cache
     */
    public int getCacheSize() {
        return clientCache.size();
    }
    
    /**
     * Vérifie si le cache est vide
     */
    public boolean isEmpty() {
        return clientCache.isEmpty();
    }
}