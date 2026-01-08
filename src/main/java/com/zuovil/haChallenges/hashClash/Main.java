package com.zuovil.haChallenges.hashClash;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuovil.haChallenges.common.CommandUtils;
import com.zuovil.haChallenges.common.FileUtils;
import com.zuovil.haChallenges.common.HttpConnector;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {

        /*
         当前md5已被破解，安全性极其不可靠。因为目前可以构造任意md5相同但是内容不同的文件执行md5攻击。
         一般而言，这种攻击模式有两种方法：直接前缀碰撞和选择前缀碰撞。
         两者区别：
         直接前缀碰撞【IPC】是指定一个前缀（任意二进制或者字符串甚至可以是程序，毕竟程序本质也是二进制字符串），碰撞出两个MD5完全一样，文件大小但内容不一样的二进制文件。
         【需要注意的是，这个MD5一样不是指源文件（或者叫前缀）和生成文件的MD5一样，单纯只是生成的两个文件MD5一样。下同。】
         另外生成的文件仅仅只是MD5一样，但不保证运行。因为填充的二进制是完全无序的，没有任何结构可言，程序或文件基本无法被运行。
         因此一般只用来快速生成MD5一样的文件，用来测试。没有什么实际应用价值。但执行时间非常快，例如对于32字节的字符串，只用几秒就构成两个MD5一样但内容不一样的文件，但对于选择前缀碰撞，需要至少十几分钟。
         两个运行数量级也有差异，选择前缀碰撞数量级是 2^45 ~ 2^50 ，直接前缀碰撞数量级是 2^20 ，数量级差了近30个数量级，无怪乎运行速度差异非常大
         【摘要上说平均复杂度为 234.1 ，可以在5分钟之内对任意的初始值求得MD5碰撞】。
         另外，选择碰撞前缀约束很大，因此在一个较小的地址进行碰撞，约束条件较多，可控制的地方也很多，使之找到符合所有要求的两个值。
         直接前缀碰撞没有那么大的约束，仅仅只是找到填充后具有两个指定md5的值，难度自然下降不少，但也导致混乱性增加，文件无法执行等问题，当然如果仅仅只是生成一个两个md5一样但是内容不同的字符串还是很容易做到的，而且很快，专业对口。

         选择前缀碰撞【CPC】，是指定一个前缀（该前缀可以是任意前缀，包括字符串或者二进制文件等待），使之在构造中满足指定结构或者说要求，生成两个MD5一样但是内容不一样的文件。
         能控制生成的无序差分到预期位置范围（长度区间，某些 bit 的差分模式 ，某些偏移位置的差异等）。是个半可控结构。
         尽管无法控制生成的差分的具体输出，但是可以控制文件的大致结构，使之这些无序差分落在关键位置上。这样尽管是两个”随机“文件，但是执行的效果不一样。
         举个例说，落在pdf的注释区，将会导致两个文件的注释不一样。落在exe的某一jump上，导致到跳转到不同的代码（这个具体跳到哪是随机的，仍就不可控，也就说生成的差分只能指定落在大致范围，但是内容是根据规则随机生成）。
         一般而言，这种半可控仍就产生很大的危害性。一般而言，具有开发权限的攻击者就一份文件提前设计，在某一部分插入恶意代码，对其进行CPC生成两个两个文件。
         一份差分落在指定范围，使之恶意代码无法运行。另一份代码差分落在其他位置，使之恶意代码可以顺利执行。攻击者先把没有攻击性的文件上传到第三方平台，审核并记录md5放入白名单发布。接着由于是自动化审核，部分系统只看文件的md5与之前的文件是否一致，如果一致就通过审核（对于大量文件来说毕竟人工审核的成本还是很高的，仅审核差异文件是个必要之策，再说那个年代还没有构造md5这种东西）。攻击者随后将可以通过精妙设计并可以执行恶意代码的文件重新上传并自动通过审核并广为流传，达到了大规模攻击的目的。
         因为md5和sha-1的不完全性，因此现在md5只用来简单校验（下载）文件的完整性（并非安全性），大部分现代系统已放弃使用md5作为安全手段，而采用sha-256或安全性更高的算法

         常见的IPC工具fastcoll，源码https://marc-stevens.nl/research/hashclash/fastcoll_v1.0.0.5-1_source.zip github搬运 https://github.com/AndSonder/fastcoll docker映像 brimstone/fastcoll
         CPC工具hashclash 作者主页https://marc-stevens.nl/p/hashclash/ github （GPU加速被禁用）https://github.com/cr-marcstevens/hashclash 支持CUDA和CELL的最后版本 https://github.com/cr-marcstevens/hashclash-old-svn-repo

         向源作者致敬！
         */
        try {
            HttpConnector httpConnector   = new HttpConnector();
            String        sendQuestionUrl = "https://hackattic.com/challenges/collision_course/problem?access_token=8d49b9708f6eb896";
            String        reply           = httpConnector.doGet(sendQuestionUrl);
            System.out.println(reply);
            JsonNode jsonNode = mapper.readTree(reply);
            String content = jsonNode.get("include").asText();

            String executeDir = System.getProperty("user.dir") + File.separator + "temp" + File.separator + "hashClash";
            String hashClashFilePath = executeDir + File.separator + "hashClash.txt";

            FileUtils.writeFile(hashClashFilePath, content);

            String command = "docker run --rm -i -v " + executeDir + ":/work -w /work brimstone/fastcoll --prefixfile hashClash.txt -o msg1.bin msg2.bin" ;

            Map<String, List<String>> out = CommandUtils.runCommandImmediate(command);
            System.out.println("out: " + out);

            String outFile1Path = executeDir + File.separator + "msg1.bin";
            String outFile2Path = executeDir + File.separator + "msg2.bin";
            byte[] msg1 = FileUtils.readFile(outFile1Path);
            byte[] msg2 = FileUtils.readFile(outFile2Path);

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(msg1);
            byte[] digest1 = md.digest();
            String content1 = new BigInteger(1, msg1).toString(16);
            String hash1 = new BigInteger(1, digest1).toString(16);
            System.out.println("collision1 content: " + content1 + "  MD5: " + hash1);

            md.update(msg2);
            byte[] digest2 = md.digest();
            String content2= new BigInteger(1, msg2).toString(16);
            String hash2 = new BigInteger(1, digest2).toString(16);
            System.out.println("collision2 content: " + content2 + "  MD5: " + hash2);
            System.out.println("内容是否相等： " + content1.equals(content2) + "  MD5是否相等: " + hash1.equals(hash2));

            String base64_1 = new String(Base64.getEncoder().encode(msg1), StandardCharsets.UTF_8);
            String base64_2 = new String(Base64.getEncoder().encode(msg2), StandardCharsets.UTF_8);
            System.out.println("base64_1: " + base64_1);
            System.out.println("base64_2: " + base64_2);
            System.out.println("原base64是否相等: " + base64_1.equals(base64_2));

            Map<String, Object> map= new HashMap<>();
            List<String> result = new ArrayList<>();
            result.add(base64_1);
            result.add(base64_2);
            map.put("files", result);
            String sendAnswerUrl = "https://hackattic.com/challenges/collision_course/solve?access_token=8d49b9708f6eb896";
            String reply2= httpConnector.doPost(sendAnswerUrl, map);
            System.out.println(reply2);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
