package com.zuovil.haChallenges.passwordHash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuovil.haChallenges.common.HttpConnector;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        HttpConnector httpConnector = new HttpConnector();
        String sendQuestionUrl = "https://hackattic.com/challenges/password_hashing/problem?access_token=8d49b9708f6eb896";
        String reply = httpConnector.doGet(sendQuestionUrl);
        System.out.println(reply);
        try {
            JsonNode jsonNode = mapper.readTree(reply);
            String saltBase64 = jsonNode.get("salt").asText();
            String password = jsonNode.get("password").asText();
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            JsonNode pbkdf2 = jsonNode.get("pbkdf2");
            int pbkdf2rounds = pbkdf2.get("rounds").asInt();
            JsonNode scrypt = jsonNode.get("scrypt");
            int N = scrypt.get("N").asInt();
            int r = scrypt.get("r").asInt();
            int p = scrypt.get("p").asInt();
            int buflen  = scrypt.get("buflen").asInt();
            String _control = scrypt.get("_control").asText();

            // 获取SHA-256算法实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 调用 digest 方法生成哈希
            byte[] sha256 = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String sha256Str  = toHex(sha256);
            System.out.println("sha256: " + sha256Str);

            // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
            SecretKeySpec secretKey = new SecretKeySpec(salt, "HmacSHA256");
            // 生成一个指定 Mac 算法 的 Mac 对象
            Mac mac = Mac.getInstance("HmacSHA256");
            // 用给定密钥初始化 Mac 对象
            mac.init(secretKey);
            // 执行Hmac计算
            byte[] hmacSHA256 =  mac.doFinal(password.getBytes(StandardCharsets.UTF_8));
            String hmacSHA256Str = toHex(hmacSHA256);
            System.out.println("hmacSHA256: " + hmacSHA256Str);

            PKCS5S2ParametersGenerator pbkdf2gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
            pbkdf2gen.init(password.getBytes(StandardCharsets.UTF_8), salt, pbkdf2rounds);
            byte[] pbkdf2Byte = ((KeyParameter) pbkdf2gen.generateDerivedParameters(256)).getKey();
            String pbkdf2Str = toHex(pbkdf2Byte);
            System.out.println("pbkdf2: " + pbkdf2Str);

            // 由于jdk自带的new PBEKeySpec不支持password的byte故转用com.bouncycastle.crypto的实现
//            KeySpec spec = new PBEKeySpec(byteToChar(hmacSHA256), salt, pbkdf2rounds, 256);
//            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
//            byte[] pbkdf2Byte = keyFactory.generateSecret(spec).getEncoded();
//            String pbkdf2Str = toHex(pbkdf2Byte);
//            System.out.println("pbkdf2: " + pbkdf2Str);

            byte[] scryptByte = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, N, r, p, buflen);
            String scryptStr = toHex(scryptByte);
            System.out.println("scrypt: " + scryptStr);

            System.out.println("_control: " + _control);

            // test
            String password1 = "rosebud";
            byte[] salt1 = "pepper".getBytes(StandardCharsets.UTF_8);
            int N1 = 128;
            int p1 = 8;
            int r1 = 4;
            byte[] scryptByte1 = SCrypt.generate(password1.getBytes(StandardCharsets.UTF_8), salt1, N1, r1, p1, buflen);
            String scryptStr1 = toHex(scryptByte1);
            System.out.println("测试scrypt: " + scryptStr1);

            System.out.println("是否相等: " + _control.equals(scryptStr1));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sha256", sha256Str);
            map.put("hmac", hmacSHA256Str);
            map.put("pbkdf2", pbkdf2Str);
            map.put("scrypt", scryptStr);
            String sendAnswerUrl = "https://hackattic.com/challenges/password_hashing/solve?access_token=8d49b9708f6eb896";
            String reply2 = httpConnector.doPost(sendAnswerUrl, map);
            System.out.println(reply2);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); // 保证每个 byte 两位 hex，前导 0 不丢
        }
        return sb.toString();
    }
}
