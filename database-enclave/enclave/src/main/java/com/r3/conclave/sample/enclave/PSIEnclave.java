package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class PSIEnclave extends Enclave {

    /**
     * This method gets called when client wants to communicate to enclave, and sends a message wrapped in a mail to host.
     * Host in turn calls deliverMail method which in turn
     * calls this method. In this method, we will deserialize the mail message, perform the computation and send the
     * result back to the clients.
     *
     * @param mail        decrypted and authenticated mail body
     * @param routingHint used by enclave to tell host whom to send the reply back to
     */
    @Override
    protected void receiveMail(EnclaveMail mail, String routingHint) {
        InputData inputData = (InputData) deserialize(mail.getBodyAsBytes());

        if (inputData != null) {
            //Retrieve the clientType from mail.
            CommandType clientType = inputData.getCommandType();
            try {
                Class.forName("org.h2.Driver");
                Connection conn = DriverManager.getConnection("jdbc:h2:~/test");
                Statement st = conn.createStatement();

                User user = inputData.getUser();

                if (clientType == CommandType.ADD) {
                    int result = st.executeUpdate("create table users(name varchar(20),  password varchar(20))");
                    System.out.println("Created table users : " + result);
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (?, ?)");

                    stmt.setString(1, user.getName());
                    stmt.setString(2, user.getPassword());

                    result = stmt.executeUpdate();
                    System.out.println("Inserted record in the database : " + result);

                    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE name = " + user.getName());
                    System.out.println("select query output : ");

                    while (rs.next()) {
                        String name = rs.getString("name");
                        String password = rs.getString("password");
                        System.out.println(name);
                        System.out.println(password);
                    }
                    byte[] encryptedReply = postOffice(mail.getAuthenticatedSender()).
                            encryptMail("Added name and password to the database".getBytes());
                    postMail(encryptedReply, routingHint);

                    postMail("Name and password added to the database".getBytes(StandardCharsets.UTF_8) , routingHint);
                } else if (clientType == CommandType.VERIFY) {
                    //verify
                    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE name = " + user.getName());
                    System.out.println("select query output : ");
                    String id = null, name = null, password = null;
                    while (rs.next()) {
                        name = rs.getString("name"); // Assuming there is a column called name.
                        password = rs.getString("password");
                        System.out.println(name);
                        System.out.println(password);
                    }
                    byte[] encryptedReply = postOffice(mail.getAuthenticatedSender()).
                            encryptMail(("Verified name and password from the database. Name is - " + name +" password is - " +password).getBytes());
                    postMail(encryptedReply, routingHint);
                }
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Using Kryo to deserialize object when passed from client to enclave
     *
     * @param data client request wrapped in mail object in bytes
     * @return deserialized input object
     */
    public static Object deserialize(byte[] data) {
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
}