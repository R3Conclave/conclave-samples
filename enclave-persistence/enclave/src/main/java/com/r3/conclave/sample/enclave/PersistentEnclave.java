package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.CommandType;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.UserData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


/**
 * This Enclave is used for password authentication.It performs 2 major tasks:
 * 1. Stores the password in Persistent Map
 * 2. Validates a given password with the stored hash.
 */
public class PersistentEnclave extends Enclave {


    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        // This is used when the host delivers a message from the client.
        // First, decode mail body as an InputData.
        InputData inputData = (InputData) deserialize(mail.getBodyAsBytes());
        if (inputData != null) {
            final byte[] result = processCommand(inputData).getBytes();
            // Get the post office object for responding back to this mail and use it to encrypt our response.
            final byte[] responseBytes = postOffice(mail).encryptMail(result);
            postMail(responseBytes, routingHint);
        }

    }

    protected Object deserialize(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is;
        try {
            is = new ObjectInputStream(in);
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    protected String processCommand(InputData inputData) {
        String result = null;
        UserData user = inputData.getUserData();
        String username = user.getUsername();
        String password = user.getPassword();

//      Example on how to store password in persistent File system
//      Use File IO operations to write/read from a file
//      If you are using conclave web host, specify the filesystem file path using the flag --filesystem.file while starting the host
//      Enable persistent file-system in your enclave's build.gradle
//        try{
//            File yourFile = new File("passwords.txt");
//            FileOutputStream fout = new FileOutputStream(yourFile, true);
//            byte[] b = password.getBytes();
//            fout.write(b);
//            fout.close();
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//        try{
//            File file = new File("passwords.txt");
//            if(file.exists()){
//                System.out.println("File found. Printing Content");
//                FileInputStream fin=new FileInputStream(file);
//                int i=0;
//                while((i=fin.read())!=-1){
//                    System.out.print((char)i);
//                }
//                fin.close();
//            }
//            else{
//                System.out.println("File not found");
//            }
//        }catch(Exception e){
//            e.printStackTrace();
//        }

        if (inputData.getCommandType() == CommandType.ADD) {
            Map<String, byte[]> persistentMap = this.getPersistentMap();
            // Store the password in Persistent map
            persistentMap.put(username, password.getBytes(StandardCharsets.UTF_8));
            result = "User added";
        } else {
            Map<String, byte[]> persistentMap = this.getPersistentMap();
            if (persistentMap.containsKey(username)) {
                result = "Invalid user credentials";
                if (Arrays.equals(persistentMap.get(username), password.getBytes(StandardCharsets.UTF_8))) {
                    result = "User authentication successful";
                }
            }
        }
        return result;
    }
}
