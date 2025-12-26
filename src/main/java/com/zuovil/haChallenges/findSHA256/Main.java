package com.zuovil.haChallenges.findSHA256;

import com.fasterxml.jackson.databind.*;
import com.zuovil.haChallenges.common.HttpConnector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        // 全局设置按字母顺序排序
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public static void main(String[] args) {
        HttpConnector httpConnector = new HttpConnector();
        String reply = httpConnector.doGet("https://hackattic.com/challenges/mini_miner/problem?access_token=8d49b9708f6eb896");
        System.out.println(reply);
        try {
            JsonNode jsonNode = mapper.readTree(reply);
            int difficulty = jsonNode.get("difficulty").asInt();
            JsonNode block  = jsonNode.get("block");
            // key按序排序
            TreeMap<String, Object> sortedJson = new TreeMap<>();
            block.properties().iterator().forEachRemaining(entry -> sortedJson.put(entry.getKey(), entry.getValue()));
            // 移除所有空格
            String blockString = mapper.writeValueAsString(sortedJson);
            System.out.println("block: " + blockString);
            // 获取SHA-256算法实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int nonce;
            for (nonce = 0; ; nonce++) {
                sortedJson.put("nonce", nonce);
                String input = mapper.writeValueAsString(sortedJson);
                digest.update(input.getBytes(StandardCharsets.UTF_8));
                byte[] hash = digest.digest();

                if (hasLeadingZeroBits(hash, difficulty)) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hash) {
                        sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
                    }
                    System.out.println("hash: " + sb + " 数据: " + input + "  difficulty: " + difficulty);
                    break;
                }
            }
            Map<String, Object> map= new HashMap<>();
            map.put("nonce", nonce);
            String sendAnswerUrl = "https://hackattic.com/challenges/mini_miner/solve?access_token=8d49b9708f6eb896";
            String reply2= httpConnector.doPost(sendAnswerUrl, map);
            System.out.println(reply2);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static boolean hasLeadingZeroBits(byte[] hash, int difficulty) {
        StringBuilder bits = new StringBuilder(hash.length * 8);

        for (byte b : hash) {
            // 坑！注意：单纯的Integer.toBinaryString(b & 0xFF)并不会将其补全到8位，需要手动格式化恢复正确解。否则会导致无限循环，因为从开始就是错的
            bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF))
                              .replace(' ', '0'));
        }

        // 只看前 difficulty 位
        for (int i = 0; i < difficulty; i++) {
            if (bits.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
