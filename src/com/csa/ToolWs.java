/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csa;

/**
 *
 * @author Carla-PC
 */

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface ToolWs {    
    @WebMethod
    public Reply testClient(String clientMainMethodName, String clientDirectoryName, DataHandler selectedFile, String selectedFileName);
            
    @WebMethod
    public Reply testClientAndServer(String clientMainMethodName, String clientDirectoryName, String serverMainMethodName, String serverDirectoryName, DataHandler selectedFile, String selectedFileName);
}