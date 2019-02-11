/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;


/**
 *
 * @author Carla-PC
 */
@WebService(
    portName = "ToolPort",
    serviceName = "ToolService",
    endpointInterface = "com.csa.ToolWs")
public class Tool implements ToolWs{   
    private static String currentWorkingDirectory = System.getProperty("user.dir") + "\\src\\com\\csa";
    private static String tempDirectory = currentWorkingDirectory + "\\Temp";
    private String userTemporaryDirectory;
    private String packageNames;
    private Thread serverThread;    
    private int localServerPort = 8888;    
    private boolean hasServerStarted;  
    private boolean hasClientStarted;
    private ArrayList<TestResult> resultList;
    
     
    private void createDirectory(String directory){
        if (!(Files.exists(Paths.get(directory)))) {
            try{
                Files.createDirectories(Paths.get(directory));
            }
            catch(Exception e){
                    
            }
        }
    }    
    
    private void createTemporayDirectories(){
        //Creation of directories
        createDirectory(tempDirectory);        
        //Unzip file in a new directory under the Temp directory
        userTemporaryDirectory = tempDirectory + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());        
        createDirectory(userTemporaryDirectory + "\\uploads");
    }        
    
    private void initializeResultList(){
        resultList = new ArrayList();
        resultList.add(new TestResult("Server did not start",false));
        resultList.add(new TestResult("Client did not start",false));
        resultList.add(new TestResult("Client did not communicat with the Server",false));
        resultList.add(new TestResult("Server did not process Client request",false));
        resultList.add(new TestResult("Client did not receive reply from the Server",false));
    }
    
    @Override
    public Reply testClient(String clientMainClassName, String clientDirectoryName, DataHandler selectedFile, String selectedFileName){
        try {
            Thread.sleep(5000);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
            
        ArrayList<String> response = new ArrayList();
        initializeResultList();  
        resultList.remove(0);
        resultList.remove(3);
        try {        
            createTemporayDirectories();        
            uploadFile(selectedFile, selectedFileName); 
            
            //Assuming that the Server is already running...
            String zipFilePath = userTemporaryDirectory + "\\uploads\\" + selectedFileName;
            unzip(zipFilePath, userTemporaryDirectory);            
                             
            response.add("[INFO] System is looking for the Client directory\n\n\n");
            
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + clientDirectoryName))){
                response.add("[INFO] System did not find the Client directory\n\n\n");                
                return EndTest( userTemporaryDirectory, response, resultList);
            }
            
            response.add("[INFO] System found the Client directory\n\n\n");
            response.add("[INFO] System is looking for the main class of the Client\n\n\n");
            
            packageNames = "";
            retrieveAndUpdatePackageName(new File (userTemporaryDirectory + "\\" + clientDirectoryName + "\\build\\classes"));

            String modifiedPackageNames = packageNames.replace('.', '\\');
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + clientDirectoryName  + "\\build\\classes\\" + modifiedPackageNames + "\\" + clientMainClassName + ".class"))){
                response.add("[INFO] System did not find the main class of the Client\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }

            response.add("[INFO] System found the main class of the Client\n\n\n");
            response.add("[INFO] System is trying to start the Client\n\n\n");
            
            hasClientStarted = true;
            
            String serverReply = runClientAndReturnOutput(userTemporaryDirectory, clientMainClassName, clientDirectoryName);

            if(!hasClientStarted){
                response.add("[INFO] Client did not start. Please check your code\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }   
            
            response.add("[INFO] Client has started\n\n\n");
            response.add("[INFO] Client is trying to communicate with the Server\n\n\n");
                
            resultList.set(0, new TestResult("Client did start",true));
            
           
            if(!serverReply.equals("")){  
                response.add("[INFO] Server has replied:\n\n");
                response.add(serverReply + "\n\n\n");
                resultList.set(1, new TestResult("Client did communicate with the Server", true));           
                resultList.set(2, new TestResult("Client did receive reply from the Server", true));  
            }
            else{                             
                response.add("[INFO] The connection has failed!\n\n\n");
                response.add("[INFO] The server that your client is trying to communicate with did not manage to process the client request or is currently down.\n\n\n");
            }    
        }
        catch(Exception ex){
            System.out.println("Error"); 
        }     
        hasServerStarted = false;
        return EndTest( userTemporaryDirectory, response, resultList);
    }
    
    @Override
    public Reply testClientAndServer(String clientMainClassName, String clientDirectoryName, String serverMainClassName, String serverDirectoryName, DataHandler selectedFile, String selectedFileName){
        try {
            Thread.sleep(5000);
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        ArrayList<String> response = new ArrayList();
        initializeResultList();
        
        try {        
            createTemporayDirectories();        
            uploadFile(selectedFile, selectedFileName);      

            
            String zipFilePath = userTemporaryDirectory + "\\uploads\\" + selectedFileName;
            unzip(zipFilePath, userTemporaryDirectory);
             
            response.add("[INFO] System is looking for the Server directory\n\n\n");
            
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + serverDirectoryName))){
                response.add("[INFO] System did not find the Server directory\n\n\n");                
                return EndTest( userTemporaryDirectory, response, resultList);
            }
            
            response.add("[INFO] System found the Server directory\n\n\n");
            response.add("[INFO] System is looking for the main class of the Server\n\n\n");
            
            System.out.println("Heeeeey");
            packageNames = "";
            retrieveAndUpdatePackageName(new File (userTemporaryDirectory + "\\" + serverDirectoryName + "\\build\\classes"));
                 
            String modifiedPackageNames = packageNames.replace('.', '\\');
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + serverDirectoryName  + "\\build\\classes\\" + modifiedPackageNames + "\\" + serverMainClassName + ".class"))){
                System.out.println(userTemporaryDirectory + "\\" + serverDirectoryName  + "\\build\\classes\\" + serverMainClassName);
                response.add("[INFO] System did not find the main class of the Server\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }

            response.add("[INFO] System found the main class of the Server\n\n\n");
            response.add("[INFO] System is trying to start the Server\n\n\n");
            
            hasServerStarted = true;

            runServer(userTemporaryDirectory, serverMainClassName, serverDirectoryName);     
            
            if(!hasServerStarted){
                response.add("[INFO] Server did not start. Please check your code\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }
            
            response.add("[INFO] Server has started\n\n\n");
            response.add("[INFO] System is checking whether the Server is listening\n\n\n");
            
            if ( !isServerListening ("localhost", localServerPort) ){
                response.add("[INFO] Server is not listening. Please ensure that server uses the port " + localServerPort + "\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }
            
            response.add("[INFO] Server is listening\n\n\n");
            resultList.set(0, new TestResult("Server did start",true));
                       
            
            response.add("[INFO] System is looking for the Client directory\n\n\n");
            
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + clientDirectoryName))){
                response.add("[INFO] System did not find the Client directory\n\n\n");                
                return EndTest( userTemporaryDirectory, response, resultList);
            }
            
            response.add("[INFO] System found the Client directory\n\n\n");
            response.add("[INFO] System is looking for the main class of the Client\n\n\n");
            
            packageNames = "";
            retrieveAndUpdatePackageName(new File (userTemporaryDirectory + "\\" + clientDirectoryName + "\\build\\classes"));
            
            modifiedPackageNames = packageNames.replace('.', '\\');
            if(!Files.exists(Paths.get(userTemporaryDirectory + "\\" + clientDirectoryName  + "\\build\\classes\\" + modifiedPackageNames + "\\" + clientMainClassName + ".class"))){
                response.add("[INFO] System did not find the main class of the Client\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }

            response.add("[INFO] System found the main class of the Client\n\n\n");
            response.add("[INFO] System is trying to start the Client\n\n\n");
            
            hasClientStarted = true;
            
            String serverReply = runClientAndReturnOutput(userTemporaryDirectory, clientMainClassName, clientDirectoryName);

            if(!hasClientStarted){
                response.add("[INFO] Client did not start. Please check your code\n\n\n");
                return EndTest( userTemporaryDirectory, response, resultList);
            }   
            
            response.add("[INFO] Client has started\n\n\n");
            
            resultList.set(1, new TestResult("Client did start",true));
            resultList.set(2, new TestResult("Client did communicate with the Server", true));
           
           
            if(!serverReply.equals("")){  
                response.add("[INFO] Server has replied:\n\n");
                response.add(serverReply + "\n\n\n");
                resultList.set(3, new TestResult("Server did process Client request", true));
                resultList.set(4, new TestResult("Client did receive reply from the Server", true));  
            }
            else{                        
                response.add("[INFO] Server did not manage to process client request\n\n\n");                
            }         
        }
        catch(Exception ex){
            System.out.println("Error");            
        }      
        
        return EndTest( userTemporaryDirectory, response, resultList);
    }
        
    
    private Reply EndTest( String directoryToBeDeleted, ArrayList<String> response, ArrayList<TestResult> resultList){
        if(hasServerStarted){
            terminateServer(directoryToBeDeleted);
        }
        delete(new File(directoryToBeDeleted));
        response.add("[INFO] Test has finished\n\n\n");
        return new Reply(response, resultList);
    }
    
    private void uploadFile(DataHandler selectedFile, String fileName){
        try {         
            InputStream input = selectedFile.getInputStream();
            OutputStream output = new FileOutputStream( new File(userTemporaryDirectory + "\\uploads\\" + fileName));

            byte[] b = new byte[100000];
            int bytesRead = 0;

            while ((bytesRead = input.read(b)) != -1) {
                output.write(b, 0, bytesRead);
            } 

        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
        private void unzip(final String zipFilePath, final String unzipLocation){
        try {            
            // Open the zip file
            ZipFile zipFile = new ZipFile(zipFilePath);
            
            Enumeration<?> enu = zipFile.entries();
            
            if (!(Files.exists(Paths.get(unzipLocation)))) {
                Files.createDirectories(Paths.get(unzipLocation));
            }            
            boolean isFirstDirectoryInTheZipFile = false;            
            
            while (enu.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enu.nextElement();
                String name = zipEntry.getName();
                                
                File file = new File(unzipLocation + "/" + name);
                
                if (name.endsWith("/")) {
                    file.mkdirs();
                    continue;
                }

                File parent = file.getParentFile();
                if (parent != null){
                    if(!isFirstDirectoryInTheZipFile){                        
                        isFirstDirectoryInTheZipFile = true;
                    }                        
                    parent.mkdirs();
                }

                // Extract the file
                InputStream is = zipFile.getInputStream(zipEntry);
                
                FileOutputStream  fos = new FileOutputStream(file);
                                
                byte[] bytes = new byte[1024];
                int length;
                while ((length = is.read(bytes)) >= 0) {
                    fos.write(bytes, 0, length);
                }
                zipEntry = null;
                fos.close();
                is.close();
            }
            zipFile.close(); 
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
        
    private void unzipx(final String zipFilePath, final String unzipLocation){
        ZipFile zipFile =  null;
        try {            
            // Open the zip file
            zipFile = new ZipFile(zipFilePath);
            
            Enumeration<?> enu = zipFile.entries();
            
            if (!(Files.exists(Paths.get(unzipLocation)))) {
                Files.createDirectories(Paths.get(unzipLocation));
            }            
            boolean isFirstDirectoryInTheZipFile = false;            
            
            while (enu.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enu.nextElement();
                String name = zipEntry.getName();
                                
                File file = new File(unzipLocation + "\\" + name);
                
                if (name.endsWith("/")) {
                    file.mkdirs();
                    continue;
                }

                File parent = file.getParentFile();
                if (parent != null){
                    if(!isFirstDirectoryInTheZipFile){                        
                        isFirstDirectoryInTheZipFile = true;
                    }                        
                    parent.mkdirs();
                }

                // Extract the file
                InputStream is = zipFile.getInputStream(zipEntry);
                
                FileOutputStream  fos = new FileOutputStream(file);
                                
                byte[] bytes = new byte[1024];
                int length;
                while ((length = is.read(bytes)) >= 0) {
                    fos.write(bytes, 0, length);
                }
                zipEntry = null;
                fos.close();
                is.close();
            }
            //System.out.println("I'm here");
            zipFile.close(); 
        } 
        catch (IOException e) {
            e.printStackTrace();
        }        
    }
    
    private void createNewBatFile(String intendedFile, ArrayList<String> commands) throws IOException {  
        File file = new File(intendedFile);
        file.createNewFile();
        PrintWriter writer = new PrintWriter(intendedFile, "UTF-8");
        for (String currentCommand:  commands){
            writer.println(currentCommand);
        }
        writer.close();
    }    
    
    private void retrieveAndUpdatePackageName(File directory){    
        File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        //Separate all the sub-directories by a final point if it is not the first directory found
        if(directories.length == 1 && packageNames.length() > 1)
            packageNames += ".";
        
        System.out.println(packageNames);
        if(directories.length == 1){            
            File firstDirectory = directories [0];
            
            //Concatenate all the sub-directories found
            packageNames += firstDirectory.getName();    
            System.out.println(packageNames);
            retrieveAndUpdatePackageName(firstDirectory);
        }   
    }
       
    private void runServer (String unzipLocation, String serverMainClassName, String serverDirectoryName) throws IOException{        
        File classesDirectory = new File(unzipLocation + "\\" + serverDirectoryName + "\\build\\classes");        
        
        
        ArrayList<String> commandsList = new ArrayList();
        commandsList.add("cd " +  classesDirectory.getPath());
        commandsList.add("java " + packageNames + "." + serverMainClassName); // assuming that this will be the publisher name required on the exam
        
        String serverBatFile = unzipLocation + "\\server.bat";
        createNewBatFile(serverBatFile, commandsList);
                 
        serverThread = new Thread(new Runnable() { //Without this thread the server process blocks the whole system            
            public void run(){               
                while (hasServerStarted && !Thread.currentThread().isInterrupted()) {
                    try {                        
                        ArrayList<ArrayList<String>> commandResultList = runBatFile("cmd /c " + serverBatFile);
                        
                        //If no errors, never gets to this line
                        ArrayList<String> outputError = commandResultList.get(0);
                        if(outputError.size() == 1){
                            hasServerStarted = false;
                        }
                    } catch (IOException ex) {
                        
                    }
                }                        
            }
        });
        serverThread.start();   
    }
    
    private boolean isServerListening(String host, int port){        
        try {
            Socket s = new Socket(host,port);
            System.out.println("Server is listening on port " + port+ " of " + host);
            s.close();
            return true;
        }
            catch (IOException ex) {
            // The remote host is not listening on this port
            System.out.println("Server is not listening on port " + port+ " of " + host);
        }
        return false;
    }
        
    private String getServerPID(String unzipLocation) throws IOException{        
        ArrayList<String> commandsList = new ArrayList();
        commandsList.add("netstat -ano | findstr :" + localServerPort); 
        String serverprocessBatFile = unzipLocation + "\\serverprocess.bat";
        createNewBatFile(serverprocessBatFile, commandsList);
        
        runBatFile("cmd /c " + serverprocessBatFile); // without this repetion the process of the server will get stuck
        ArrayList<ArrayList<String>> commandResultList = runBatFile("cmd /c " + serverprocessBatFile);         
       
                        
        ArrayList<String> outputSucceed = commandResultList.get(1);
               
        if(outputSucceed.size() > 2){            
            String[] parts = outputSucceed.get(2).split("LISTENING"); 
            return parts[1].trim();            
        }
        return "-1";
    }
    
    private void terminateServer(String unzipLocation){        
        try{
            String serverPID = getServerPID (unzipLocation);
            serverThread.interrupt();
            String serverKillerProcessBatFile = unzipLocation + "\\serverkillerprocess.bat";
            
            ArrayList<String> commandsList = new ArrayList();
            System.out.println("server pid : " + serverPID);
            commandsList.add("taskkill /PID " + serverPID + " /F");   
            
            createNewBatFile(serverKillerProcessBatFile, commandsList);
            runBatFile("cmd /c " + serverKillerProcessBatFile);
        }
        catch(Exception ex){}
    }        
    
    private  String runClientAndReturnOutput(String unzipLocation, String clientMainClassName, String clientDirectoryName) throws IOException{   
        File classesDirectory = new File(unzipLocation + "\\" + clientDirectoryName + "\\build\\classes"); 
             
        ArrayList<String>  commandsList = new ArrayList();
        commandsList.add("cd " +  classesDirectory.getPath());
        commandsList.add("java " + packageNames + "." + clientMainClassName); 
               
        String clientBatFile = unzipLocation + "\\client.bat";
        createNewBatFile(clientBatFile, commandsList);       
        
        ArrayList<ArrayList<String>> commandResultList = runBatFile("cmd /c " + clientBatFile );
        ArrayList<String> outputError = commandResultList.get(0);
       
        if(outputError.size() == 1){ // This means main class has crushed
            hasClientStarted = false;
            return "";
        }
                
        ArrayList<String> outputSucceed = commandResultList.get(1);
        
        if(outputSucceed.size() > 4 && outputSucceed.size() < 30){
            return outputSucceed.get(4);
        }
        
        return "";
    }
       
    private  ArrayList<ArrayList<String>> runBatFile(String filePath) throws IOException{
        Runtime rt = Runtime.getRuntime();        	
        Process proc = rt.exec(filePath);
       
        String s;
        ArrayList<ArrayList<String>> commandResultList = new ArrayList(); 
        ArrayList<String> outputError = new ArrayList();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));        
        while ((s = br.readLine()) != null) {
            System.out.println(s);
            outputError.add(s);
        }          
        commandResultList.add(outputError);
        
        ArrayList<String> outputSucceed = new ArrayList();
        br = new BufferedReader(new InputStreamReader(proc.getInputStream()));        
        while ((s = br.readLine()) != null) {
            System.out.println(s);
            outputSucceed.add(s);
        }  
        
        commandResultList.add(outputSucceed);
        
        return commandResultList;
    }   
        
    private static void delete(File file){ 
    	if(file.isDirectory()){ 
            //directory is empty, then delete it
            if(file.list().length==0){    			
               file.delete();
            }
            else{
               //list all the directory contents
               String files[] = file.list();
               for (String temp : files) {
                  //construct the file structure
                  File fileDelete = new File(file, temp);
                  //recursive delete
                 delete(fileDelete);
               }
               //check the directory again, if empty then delete it
               if(file.list().length==0){
                 file.delete();
               }
            }    		
    	}else{
            //if file, then delete it
            file.delete();
    	}
    }
        
    public static void main(String[] args) {
        delete(new File(tempDirectory));
        Endpoint.publish("http://localhost:8558/service", new Tool());
    } 
}