package com.r3.conclave.sample.auction.client.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auction-service")
public class AuctionController {

    @Autowired
    AuctionService auctionService;

    @PostMapping("/connect")
    public ResponseModel connectToHost(@RequestBody ConnectionRequest connectionRequest){
        try {
            auctionService.connectToHost(connectionRequest);
            return new ResponseModel(true, "Connection Successful");
        }catch (AppException ae){
            return new ResponseModel(false, ae.getMessage());
        }
    }

    @PostMapping("/bid")
    public ResponseModel sendBid(@RequestBody BidRequest bidRequest){
        try {
            auctionService.sendMail(bidRequest);
            return new ResponseModel(true, "Bid Submitted Successfully");
        }catch (AppException ae){
            return new ResponseModel(false, ae.getMessage());
        }
    }

    @PostMapping("/poll")
    public ResponseModel pollResponse(){
        try {
            String response = auctionService.pollMail();
            return new ResponseModel(true, response);
        }catch (AppException ae){
            return new ResponseModel(false, ae.getMessage());
        }
    }

    @PostMapping("/endAuction")
    public ResponseModel processBids(){
        try {
            BidRequest bidRequest = new BidRequest();
            bidRequest.setRoleType("ADMIN");
            String response = auctionService.sendMail(bidRequest);
            return new ResponseModel(true, response);
        }catch (AppException ae){
            return new ResponseModel(false, ae.getMessage());
        }
    }
}
