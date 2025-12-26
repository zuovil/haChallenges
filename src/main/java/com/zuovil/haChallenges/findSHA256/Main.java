package com.zuovil.haChallenges.findSHA256;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuovil.haChallenges.common.HttpConnector;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        HttpConnector httpConnector = new HttpConnector();
        String reply = httpConnector.doGet("https://hackattic.com/challenges/mini_miner/problem?access_token=8d49b9708f6eb896");
        System.out.println(reply);
    }
}
