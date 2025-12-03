package com.example.soap.service;

import com.example.soap.model.Client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.List;

@WebService
public interface ClientSoapService {
    
    @WebMethod
    List<Client> getAllClients();
    
    @WebMethod
    Client getClientById(@WebParam(name = "id") Long id);
    
    @WebMethod
    Client getClientByEmail(@WebParam(name = "email") String email);
    
    @WebMethod
    Client createClient(@WebParam(name = "client") Client client);
    
    @WebMethod
    Client updateClient(@WebParam(name = "id") Long id, @WebParam(name = "client") Client client);
    
    @WebMethod
    boolean deleteClient(@WebParam(name = "id") Long id);
    
}
