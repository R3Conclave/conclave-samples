package com.r3.conclave.sample.auction.client.springboot;

import com.r3.conclave.client.EnclaveClient;
import com.r3.conclave.client.web.WebEnclaveTransport;
import com.r3.conclave.common.EnclaveConstraint;
import com.r3.conclave.mail.EnclaveMail;
import com.r3.conclave.sample.auction.common.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Service
public class AuctionService {
    private EnclaveClient enclaveClient;
    private EnclaveConstraint enclaveConstraint;
    private WebEnclaveTransport webEnclaveTransport;
    private EnclaveMail response;

    public void connectToHost(ConnectionRequest connectionRequest) throws AppException{
        try {
            enclaveConstraint = EnclaveConstraint.parse(connectionRequest.getConstraint());
            enclaveClient = new EnclaveClient(enclaveConstraint);
            webEnclaveTransport = new WebEnclaveTransport(connectionRequest.getHostUrl());
            enclaveClient.start(webEnclaveTransport);
        }catch (Exception e){
            throw new AppException(e.getMessage());
        }
    }

    public String sendMail(BidRequest bidRequest) throws AppException{
        try {
            byte[] serializedOutput = serializeMessage(bidRequest.getRoleType(), bidRequest.getBid());

            // Send mail to enclave and receive response
            response = enclaveClient.sendMail(serializedOutput);
            if(response !=null)
                return new String((response.getBodyAsBytes()));
            else
                return null;
        }catch (Exception e){
            throw new AppException(e.getMessage());
        }
    }

    public String pollMail() throws AppException {
        try {
            if(response == null){
                response = enclaveClient.pollMail();
            }

            if(response !=null)
                return new String((response.getBodyAsBytes()));
            else {
                return null;
            }
        }catch (Exception e){
            throw new AppException(e.getMessage());
        }
    }

    private byte[] serializeMessage(String messageType, int bid) {
        Message message = new Message(messageType, bid);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os;
        try {
            os = new ObjectOutputStream(out);
            os.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
