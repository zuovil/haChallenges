package com.zuovil.haChallenges.readingQR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private final static ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) {
        try(CloseableHttpClient client  = HttpClients.createDefault()){
            HttpGet getImageUrl  = new HttpGet(new URI("https://hackattic.com/challenges/reading_qr/problem?access_token" +
                                                           "=8d49b9708f6eb896"));
            try{
                CloseableHttpResponse response = client.execute(getImageUrl);
                // 检查响应状态码
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    String jsonString = EntityUtils.toString(entity);
                    String imageUrl = mapper.readTree(jsonString).get("image_url").asText();
                    HttpGet getImage = new HttpGet(new URI(imageUrl));
                    CloseableHttpResponse response1 = client.execute(getImage);
                    if (response1.getCode() == 200) {
                        HttpEntity entity1 = response1.getEntity();
                        // 将响应实体转换为字节数组
                        byte[] imageData = EntityUtils.toByteArray(entity1);

                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                        // 使用LuminanceSource和Binarizer将图片转化为二进制数据
                        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        // 创建一个解码器,并解码
                        Result qrResult = new MultiFormatReader().decode(bitmap);
                        String result = qrResult.getText();

                        HttpPost sendReply = new HttpPost(new URI("https://hackattic.com/challenges/reading_qr/solve?access_token" +
                                                                         "=8d49b9708f6eb896"));
                        sendReply.addHeader("Content-Type", "application/json");
                        Map<String, String> map = new HashMap<>();
                        map.put("code", result);
                        sendReply.setEntity(new StringEntity(mapper.writeValueAsString(map)));


                        CloseableHttpResponse response2 = client.execute(sendReply);

                        // 读取响应内容
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response2.getEntity().getContent()));
                        String line;
                        StringBuilder receive = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            receive.append(line);
                        }

                        // 打印响应内容
                        System.out.println("last Response: " + receive);

                    }

                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
