package com.example.soap.service;

import com.example.soap.annotation.MonitorSoapMethod;
import com.example.soap.cache.ClientCacheService;
import com.example.soap.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jws.WebService;
import java.util.List;

@Service
@WebService(endpointInterface = "com.example.soap.service.ClientSoapService")
public class ClientSoapServiceImpl implements ClientSoapService {
    
    @Autowired
    private ClientCacheService cacheService;
    
    @Override
    @MonitorSoapMethod("getAllClients")
    public List<Client> getAllClients() {
        return cacheService.getAllClients();
    }
    
    @Override
    @MonitorSoapMethod("getClientById")
    public Client getClientById(Long id) {
        return cacheService.getClientById(id);
    }
    
    @Override
    @MonitorSoapMethod("getClientByEmail")
    public Client getClientByEmail(String email) {
        return cacheService.getClientByEmail(email);
    }
    
    @Override
    @MonitorSoapMethod("createClient")
    public Client createClient(Client client) {
        // Dans ce mode, on ne crée pas vraiment en base
        // On ajoute juste au cache (temporaire jusqu'au prochain reload)
        if (client.getId() == null) {
            // Générer un ID temporaire
            client.setId(System.currentTimeMillis());
        }
        cacheService.putClient(client);
        return client;
    }
    
    @Override
    @MonitorSoapMethod("updateClient")
    public Client updateClient(Long id, Client client) {
        Client existingClient = cacheService.getClientById(id);
        if (existingClient != null) {
            existingClient.setNom(client.getNom());
            existingClient.setPrenom(client.getPrenom());
            existingClient.setEmail(client.getEmail());
            existingClient.setTelephone(client.getTelephone());
            existingClient.setAdresse(client.getAdresse());
            existingClient.setVille(client.getVille());
            existingClient.setCodePostal(client.getCodePostal());
            cacheService.putClient(existingClient);
            return existingClient;
        }
        return null;
    }
    
    @Override
    @MonitorSoapMethod("deleteClient")
    public boolean deleteClient(Long id) {
        return cacheService.deleteClient(id);
    }
}