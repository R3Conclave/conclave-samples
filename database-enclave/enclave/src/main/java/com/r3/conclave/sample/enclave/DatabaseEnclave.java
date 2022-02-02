package com.r3.conclave.sample.enclave;

import com.r3.conclave.enclave.Enclave;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.common.CommandType;
import com.r3.conclave.sample.common.InputData;
import com.r3.conclave.sample.common.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;

/**
 * This enclave shows how to use an H2 database inside an enclave. Depending on the command type we will create, insert or select
 * database records from the database. The I/O operations are mapped to a persistent file on the host system.
 */
public class DatabaseEnclave extends Enclave {
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
            String reply = null;
            String userName = null;
            String password = null;

            CommandType commandType = inputData.getCommandType();
            try {
                Class.forName("org.h2.Driver");
                Connection conn = DriverManager.getConnection("jdbc:h2:~/test");
                Statement st = conn.createStatement();

                User user = inputData.getUser();

                if (commandType == CommandType.CREATE) {
                    st.executeUpdate("create table if not exists users(name varchar(20),  password varchar(20))");
                    System.out.println("Created table users if table does not exists");

                    reply = "Users table created in the database";
                    postReplyToClient(mail, routingHint, reply);

                } else if (commandType == CommandType.ADD) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (?, ?)");

                    stmt.setString(1, user.getName());
                    stmt.setString(2, user.getPassword());

                    int result = stmt.executeUpdate();
                    System.out.println("Inserted record in the database : " + result);

                    reply = retrieveUserFromDb(st, user);
                    reply = reply + "Added name and password to the database.";
                    postReplyToClient(mail, routingHint, reply);

                } else if (commandType == CommandType.VERIFY) {
                    //retrieve the records from the database when the user passes the VERIFY command and send it back to the client
                    reply = retrieveUserFromDb(st, user);
                    reply += "Verified name and password from the database.";
                    postReplyToClient(mail, routingHint, reply);

                    conn.prepareStatement("SHUTDOWN").executeUpdate();
                    conn.close();
                }
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String retrieveUserFromDb(Statement st, User user) throws SQLException {
        String userName = null;
        String password = null;
        ResultSet rs = st.executeQuery("SELECT * FROM users WHERE name = '" + user.getName() + "'");
        System.out.println("select query output : ");

        while (rs.next()) {
            userName = rs.getString("name");
            password = rs.getString("password");
            System.out.println(userName);
            System.out.println(password);
        }
        return "Users Name is - " + userName + " password is - " + password + " .";
    }

    private void postReplyToClient(EnclaveMail mail, String routingHint, String reply) {

        byte[] encryptedReply = postOffice(mail.getAuthenticatedSender()).
                encryptMail(reply.getBytes());
        postMail(encryptedReply, routingHint);
    }
}