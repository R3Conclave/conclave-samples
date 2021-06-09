package com.r3.conclave.sample.client;

import com.r3.conclave.sample.common.AdDetails;
import com.r3.conclave.sample.common.UserDetails;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class tert {

    public static void main(String args[]) throws IOException {
        //
//        if("MERCHANT".equals(type)) {
//            System.out.println(type);
//            List<UserDetails> userDetailsList = new ArrayList();
//
//            while (true) {
//                String ccNo = in.next();
//                UserDetails userDetails = new UserDetails(ccNo);
//                userDetailsList.add(userDetails);
//            }
//        } else if("SERVICE-PROVIDER".equals(type)) {
//            System.out.println(type);
//        }
//
//
//
//        List<UserDetails> userDetailsList = null;
//        List<AdDetails> adDetailsList= null;
//        Scanner command = new Scanner(System.in);
//        BufferedReader reader =
//                new BufferedReader(new InputStreamReader(System.in));
//        String bidAmount = reader.readLine();
//
//        String type = args[0];
//
//        if("MERCHANT".equals(bidAmount)) {
//            userDetailsList = new ArrayList();
//            boolean running = true;
//            do {
//                System.out.println("Enter command: ");
//                String creditCardNumber = reader.readLine();
//                switch(creditCardNumber){
//
//                    case "exit":
//                        System.out.println("Application Closed");
//                        running = false;
//                        break;
//                    default:
//                        System.out.println("Enter User Credit card Number!");
//                        UserDetails userDetails = new UserDetails(creditCardNumber);
//                        userDetailsList.add(userDetails);
//                        break;
//                }
//            } while(running);
//        } else if("SERVICE-PROVIDER".equals(bidAmount)) {
//            adDetailsList = new ArrayList();
//            boolean running = true;
//            do {
//                System.out.println("Enter command: ");
//                String creditCardNumber = reader.readLine();
//                switch(creditCardNumber){
//
//                    case "exit":
//                        System.out.println("Application Closed");
//                        running = false;
//                        break;
//                    default:
//                        System.out.println("Enter User Credit card Number!");
//                        AdDetails adDetails = new AdDetails(creditCardNumber);
//                        adDetailsList.add(adDetails);
//                        break;
//                }
//            } while(running);
//        }
//
//        System.out.println(userDetailsList.size());
//        command.close();


        List<UserDetails> userDetailsList = new ArrayList();
        UserDetails userDetails1 = new UserDetails("123");
        UserDetails userDetails2 = new UserDetails("456");
        userDetailsList.add(userDetails1);
        userDetailsList.add(userDetails2);

        List<AdDetails> adDetailsList = new ArrayList();
        AdDetails adDetails1 = new AdDetails("123");
        AdDetails adDetails2 = new AdDetails("456");
        AdDetails adDetails3 = new AdDetails("4561");
        AdDetails adDetails4 = new AdDetails("4562");

        adDetailsList.add(adDetails1);
        adDetailsList.add(adDetails2);
        adDetailsList.add(adDetails2);
        adDetailsList.add(adDetails2);



        List<String> merchantCreditCardNumbers = new ArrayList<>();
        for (UserDetails userDetails : userDetailsList) {
            merchantCreditCardNumbers.add(userDetails.getCreditCardNumber());
        }

        List<String> serviceProviderCreditCardNumbers = new ArrayList<>();
        for (AdDetails adDetails : adDetailsList) {
            serviceProviderCreditCardNumbers.add(adDetails.getCreditCardNumber());
        }

        Set<String> usersWhoPurchasedAfterClickingAd = serviceProviderCreditCardNumbers.stream()
                .distinct()
                .filter(merchantCreditCardNumbers::contains)
                .collect(Collectors.toSet());

        double adConversionRate = (new Double(usersWhoPurchasedAfterClickingAd.size()) / adDetailsList.size());


        System.out.println(adConversionRate);
    }
}
