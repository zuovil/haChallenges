package com.zuovil.haChallenges.byteUnPack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try(CloseableHttpClient client  = HttpClients.createDefault()){
            HttpGet getBase64Code = new HttpGet(new URI("https://hackattic.com/challenges/help_me_unpack/problem" +
                                                         "?access_token=8d49b9708f6eb896"));
            CloseableHttpResponse response = client.execute(getBase64Code);
            if(response.getCode() == 200){
                HttpEntity entity = response.getEntity();
                String jsonString = EntityUtils.toString(entity);
                String baseString = mapper.reader().readTree(jsonString).get("bytes").asText();
                byte[] byteArray = Base64.getDecoder().decode(baseString);
                // 将字节数组加载进缓冲区
                ByteBuffer buffer = ByteBuffer.wrap(byteArray);
                // 内存读取从低位到高位 ，e.g int = 1 00000001(2) 小端序从低到高 01 00 00 00 大端序从高到低 00 00 00 01
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                // 下四位读int，以此类推
                int i = buffer.getInt();
                long l = Integer.toUnsignedLong(buffer.getInt());
                short s = buffer.getShort();

                // C语言结构体和java里面不一样，它有字节对齐机制。对于float它总会对齐到4字节。由于short占两个字节，所以会空出来2字节。因此需要在这里跳过两个字节（这就是所谓的四字节对齐）
                // 题目描述的很清楚 In case you're wondering, we're using 4 byte , so everything is in the context of a 32-bit platform
                // 结构体(struct)的数据成员,第一个数据成员存放的地址为结构体变量偏移量为0的地址处。其他结构体成员自身对齐时,存放的地址为min{有效对齐值为自身对齐值, 指定对齐值} 的最小整数倍的地址处.
                // 自身对齐值:结构体变量里每个成员的自身大小;指定对齐值:有宏 #pragma pack(N) 指定的值,这里面的 N一定是2的幂次方.如1,2,4,8,16等.如果没有通过宏那么在32位Linux主机上默认指定对齐值为4,64位的默认对齐值为8,AMR CPU默认指定对齐值为8;
                // 有效对齐值:结构体成员自身对齐时有效对齐值为自身对齐值与指定对齐值中 较小的一个
                // 总体对齐时,字节大小是min{所有成员中自身对齐值最大的, 指定对齐值} 的整数倍.#pragma pack(N) 每个特定平台上的编译器都有自己的默认“对齐系数”(也叫对齐模数)。程序员可以通过预编译命令#pragma pack(n)，n=1,2,4,8,16来改变这一系数，其中的n就是你要指定的“对齐系数”
                buffer.position(buffer.position() + 2);

                // C 结构体内存布局 ≠ Java 顺序读字节.C 结构体有 ABI 对齐（alignment / padding），Java ByteBuffer 没有。
                // short → float 是 最危险的对齐点;必然发生short (2 bytes) padding (2 bytes) float (4 bytes)
                float f = buffer.getFloat();
                double d = buffer.getDouble();

                buffer.order(ByteOrder.BIG_ENDIAN);
                double d2 = buffer.getDouble();
                Map<String, Number> map = new LinkedHashMap<>();
                map.put("int", i);
                map.put("uint", l);
                map.put("short", s);
                map.put("float", f);
                map.put("double", d);
                map.put("big_endian_double", d2);

                String jsonStr = mapper.writer().writeValueAsString(map);
                System.out.println(jsonStr);

                HttpPost sendReply = new HttpPost(new URI("https://hackattic.com/challenges/help_me_unpack/solve?access_token=8d49b9708f6eb896"));
                sendReply.addHeader("Content-Type", "application/json");
                sendReply.setEntity(new StringEntity(jsonStr));

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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
