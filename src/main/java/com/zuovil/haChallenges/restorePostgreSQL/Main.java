package com.zuovil.haChallenges.restorePostgreSQL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.zuovil.haChallenges.common.Command;
import com.zuovil.haChallenges.common.DealProcessStream;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Main {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet getDump = new HttpGet(new URI("https://hackattic.com/challenges/backup_restore/problem?access_token=8d49b9708f6eb896"));
            CloseableHttpResponse response = client.execute(getDump);
            if (response.getCode() == 200) {
                HttpEntity entity     = response.getEntity();
                String     jsonString = EntityUtils.toString(entity);
                String     baseString = mapper.reader().readTree(jsonString).get("dump").asText();
                byte[] byteArray = Base64.getDecoder().decode(baseString);
                String dumpFilePath = System.getProperty("user.dir") + File.separator + "temp" + File.separator +
                        "mydb.sql";
                try (
                        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(byteArray));
                        FileOutputStream fos = new FileOutputStream(dumpFilePath)
                ) {
                    File   dumpFile  = new File(dumpFilePath);
                    if (!dumpFile.getParentFile().exists()) {
                        dumpFile.getParentFile().mkdirs();
                    }
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = gis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                String clearPostgreSQL = "for /F %i in ('docker ps -aq') do (docker stop %i && docker rm -f %i) && docker volume prune -f";
                Map<String, List<String>> out1 = runCommandImmediate(clearPostgreSQL);
                // -e 添加环境变量
                String runPostgreSQL = "docker run -d -p 5432:5432 --name postgresql -v " +
                        "pgdata:/var/lib/postgresql/data -e POSTGRES_HOST_AUTH_METHOD=trust postgres:10.19";
                Map<String, List<String>> out2 = runCommand(runPostgreSQL);
                System.out.println("out1: " + out1);
                System.out.println("out2: " + out2);
                // 等待启动完成
                for (int i = 0; i < 30; i++) {
                    Map<String, List<String>> ready =
                            runCommand("docker exec postgresql pg_isready -U postgres");
                    if (ready.toString().contains("accepting connections")) {
                        break;
                    }
                    Thread.sleep(1000);
                }
                String copyDump = "docker cp \"" + dumpFilePath + "\" postgresql:/tmp/mydb.sql";
                Map<String, List<String>> out3 = runCommand(copyDump);
                System.out.println("out3: " + out3);
                // docker exec [OPTIONS] CONTAINER COMMAND [ARG...]
                String createDateBase = "docker exec postgresql createdb -h localhost -p 5432 -U postgres mydb";
                Map<String, List<String>> out4 = runCommand(createDateBase);
                System.out.println("out4: " + out4);
                String restoreData =
                        "docker exec postgresql psql -U postgres -d mydb -f /tmp/mydb.sql";
                Map<String, List<String>> out5 = runCommand(restoreData);
                System.out.println("out5: " + out5);
                PostgresqlConnector connector = new PostgresqlConnector();
                String querySql = "select * from criminal_records";
                String result = connector.query(querySql);
                //PostgresqlConnector.close();
                JsonNode node = mapper.reader().readTree(result);
                System.out.println("JsonNode: " + node);
                List<String> ssnList = new ArrayList<>();
                if(node.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) node;
                    arrayNode.forEach((x) -> {
                        if("alive".equals(x.get("status").asText()))
                            ssnList.add(x.get("ssn").asText());});
                }

                System.out.println("ssnList no: " + ssnList.size());
                Map<String, List<String>> map = new LinkedHashMap<>();
                map.put("alive_ssns", ssnList);

                String jsonStr = mapper.writer().writeValueAsString(map);
                System.out.println(jsonStr);

                HttpPost sendReply = new HttpPost(new URI("https://hackattic.com/challenges/backup_restore/solve?access_token=8d49b9708f6eb896"));
                sendReply.addHeader("Content-Type", "application/json");
                sendReply.setEntity(new StringEntity(jsonStr));

                CloseableHttpResponse response2 = client.execute(sendReply);

                // 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(response2.getEntity().getContent()));
                String         line;
                StringBuilder receive = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    receive.append(line);
                }

                String finishRun = "docker stop postgresql";
                // 打印响应内容
                System.out.println("last Response: " + receive);
                runCommand(finishRun);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static Map<String, List<String>> runCommand(String commandString) throws Exception {
        Command command     = Command.fromInput(commandString);
        String  commandName = command.getCommandName();
        List<String> params = command.getArgs();
        List<String> commands = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("win")){
            commands.add("cmd");
            commands.add("/c");
        } else {
            commands.add("sh");
            commands.add("-c");
        }

        params.add(0, commandName);
        String trueCommand = String.join(" ", params);
        commands.add(trueCommand);
        // cmd -c 允许用户在命令行中直接运行一段字符串形式的 Shell 脚本或命令
        Process           process = new ProcessBuilder(commands).start();
        DealProcessStream out     = new DealProcessStream(process.getInputStream());
        out.start();
        out.join();
        DealProcessStream err = new DealProcessStream(process.getErrorStream());
        err.start();
        err.join();
        process.waitFor();
        List<String> outList = out.getOut();
        List<String> errList = err.getOut();
        Map<String, List<String>> map = new HashMap<>();
        map.put("out", outList);
        map.put("err", errList);
        return map;
    }

    // 允许直接运行命令而不经过解析
    private static Map<String, List<String>> runCommandImmediate(String commandString) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        if(commandString == null || commandString.isEmpty()) {
            result.put("out", new ArrayList<>());
            result.put("err", new ArrayList<>());
        }
        List<String> commands = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("win")){
            commands.add("cmd");
            commands.add("/c");
        } else {
            commands.add("sh");
            commands.add("-c");
        }

        commands.add(commandString);
        // cmd -c 允许用户在命令行中直接运行一段字符串形式的 Shell 脚本或命令
        Process process = new ProcessBuilder(commands).start();
        DealProcessStream out = new DealProcessStream(process.getInputStream());
        out.start();
        out.join();
        DealProcessStream err = new DealProcessStream(process.getErrorStream());
        err.start();
        err.join();
        process.waitFor();
        List<String> outList = out.getOut();
        List<String> errList = err.getOut();
        result.put("out", outList);
        result.put("err", errList);
        return result;
    }

    public static Map<String, String> getEnv() {
        Map<String, String> env = new HashMap<>();
        String osName = System.getProperty("os.name").toLowerCase();
        String PATH = System.getenv("PATH");
        String[] paths;
        if(osName.contains("win")){
            paths = PATH.split(";");
        } else {
            paths = PATH.split(":");
        }

        for(String path : paths){
            File parent = new File(path);
            if(parent.exists()) {
                if(parent.isDirectory()) {
                    Map<String, String> fileNameMap = Arrays.stream(Objects.requireNonNull(parent.listFiles((dir, name) -> new File(dir, name).isFile())))
                                                            .filter(File::canExecute)
                                                            .collect(Collectors.toMap(file -> file.getName().lastIndexOf(".") != -1 ?
                                                                                              file.getName().substring(0, file.getName().lastIndexOf(".")) : file.getName(),
                                                                                      File::getAbsolutePath,
                                                                                      (v1, v2) -> v2));
                    if(!fileNameMap.isEmpty()){
                        env.putAll(fileNameMap);
                    }
                } else {
                    if(parent.canExecute()) {
                        String fileName = parent.getName().lastIndexOf(".") != -1 ?
                                parent.getName().substring(0, parent.getName().lastIndexOf(".")) : parent.getName();
                        env.put(fileName, parent.getAbsolutePath());
                    }
                }
            }
        }
        return env;
    }

    public static String getExecPath(String commandName, Map<String, String> env) {
        if(commandName == null || env == null) {
            return null;
        }
        List<String> similarCommandList = env.keySet().stream().filter(s -> s.startsWith(commandName)).sorted().collect(Collectors.toList());
        if(similarCommandList.isEmpty()) {
            return null;
        }
        return similarCommandList.get(0);
    }
}
