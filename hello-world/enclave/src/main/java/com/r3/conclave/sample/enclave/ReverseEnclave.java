package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Simply reverses the bytes that are passed in.
 */
public class ReverseEnclave extends Enclave {
    // We store the previous result to showcase that the enclave internals can be examined in a mock test.
    byte[] previousResult;


    @Override
    protected byte[] receiveFromUntrustedHost(byte[] bytes) {
        // This is used for host->enclave calls so we don't have to think about authentication.
        final var input = new String(bytes);
        var result = reverse(input).getBytes();
        previousResult = result;
        return result;
    }

    private static String reverse(String input) {
        var builder = new StringBuilder(input.length());
        for (var i = input.length() - 1; i >= 0; i--)
            builder.append(input.charAt(i));
        return builder.toString();
    }

    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        // This is used when the host delivers a message from the client.
        // First, decode mail body as a String.
        var stringToReverse = new String(mail.getBodyAsBytes());

        //Example on how to store information in persistent map
        //For demonstration: Stores the last input string
        Map<String, byte[]> persistentMap = this.getPersistentMap();
        if(persistentMap.containsKey("LAST-STRING")){
            String t = new String(persistentMap.get("LAST-STRING"), StandardCharsets.UTF_8);
            System.out.println("Printing value from Persistent Map");
            System.out.println(t);
        }
        persistentMap.put("LAST-STRING", stringToReverse.getBytes(StandardCharsets.UTF_8));

        // Reverse it and re-encode to UTF-8 to send back.
        final var reversedEncodedString = reverse(stringToReverse).getBytes();
        // Get the post office object for responding back to this mail and use it to encrypt our response.
        final var responseBytes = postOffice(mail).encryptMail(reversedEncodedString);

        //Example on how to store information in persistent File system
        //For demonstration: Stores the all last input string
        System.out.println("Persistent File System");
        System.out.println(" Step 1: Writing to file");
        try{
            File yourFile = new File("inputs.txt");
            FileOutputStream fout = new FileOutputStream(yourFile, true);
            byte[] b = stringToReverse.getBytes();
            fout.write(b);
            fout.write(System.getProperty("line.separator").getBytes());
            fout.close();
        }catch(Exception e){
            System.out.println("Exception: Writing to file failed.");
            e.printStackTrace();
        }

        System.out.println("Step 2: Reading from File");
        try{
            File file = new File("inputs.txt");
            if(file.exists()){
                System.out.println("File found. Printing Content");
                FileInputStream fin=new FileInputStream(file);
                int i=0;
                while((i=fin.read())!=-1){
                    System.out.print((char)i);
                }
                fin.close();
            }
            else{
                System.out.println("File not found");
            }

        }catch(Exception e){System.out.println("Exception: Reading from file failed.");
        e.printStackTrace();
        }

    postMail(responseBytes, routingHint);
    }
}
