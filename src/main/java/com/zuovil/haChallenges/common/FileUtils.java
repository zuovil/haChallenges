package com.zuovil.haChallenges.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static void createFile(String filePath) {
        if(filePath == null || filePath.isEmpty()) {
            return;
        }
        File file = new File(filePath);
        File parentFile = new File(file.getParent());
        if(!parentFile.exists()) {
            parentFile.mkdirs();
        }
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void copyFile(String fromPath, String toPath) {
        if(fromPath == null || fromPath.isEmpty()) {
            return;
        }
        if(toPath == null || toPath.isEmpty()) {
            return;
        }

        File from = new File(fromPath);
        if(!from.exists()) {
            return;
        }
        File to = new File(toPath);
        File parent = new File(to.getParent());
        if(!parent.exists()) {
            parent.mkdirs();
        }

        try(BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(from.toPath())));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(to.toPath())));) {
            if(!to.exists()) {
                to.createNewFile();
            }

            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void copyDir(String fromPath, String toPath) {
        if(fromPath == null || fromPath.isEmpty()) {
            return;
        }
        if(toPath == null || toPath.isEmpty()) {
            return;
        }
        File from = new File(fromPath);
        if(!from.exists() || !from.isDirectory()) {
            return;
        }
        File to = new File(toPath);
        File parent = new File(to.getParent());
        if(!parent.exists()) {
            parent.mkdirs();
        }
        if(!to.exists()) {
            to.mkdir();
        }
        try {
            Files.walk(Paths.get(fromPath))
                 .forEach(source -> {
                     Path destination = Paths.get(toPath, source.toString().substring(fromPath.length()));
                     try {
                         Files.copy(source, destination);
                     } catch (IOException e) {
                         System.out.println(e.getMessage());
                     }
                 });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(String filePath, String content) {
        createFile(filePath);
        if(content == null || content.isEmpty()) {
            return;
        }
        try {
            Files.write(Paths.get(filePath), content.getBytes());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static byte[] readFile(String filePath) {
        if(filePath == null || filePath.isEmpty()) {
            return null;
        }
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(filePath));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return bytes;
    }

    public static String toUnixPath(String windowsPath) {
        // 替换 \ 为 /
        String path = windowsPath.replace("\\", "/");
        // 将盘符 F:/ -> /f/
        if (path.length() > 2 && path.charAt(1) == ':') {
            char drive = Character.toLowerCase(path.charAt(0));
            path = "/" + drive + path.substring(2);
        }
        return path;
    }
}
