package com.zuovil.haChallenges.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandUtils {

    public static Map<String, List<String>> runCommand(String commandString) throws Exception {
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
        System.out.println(commands);
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

    public static Map<String, List<String>> runCommandImmediate(String commandString) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        if(commandString == null || commandString.isEmpty()) {
            result.put("out", new ArrayList<>());
            result.put("err", new ArrayList<>());
            return result;
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

    public static Map<String, List<String>> runCommandWithoutCMD(String commandString) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        if(commandString == null || commandString.isEmpty()) {
            result.put("out", new ArrayList<>());
            result.put("err", new ArrayList<>());
            return result;
        }
        List<String> commands = new ArrayList<>();
        Command command     = Command.fromInput(commandString);
        String  commandName = command.getCommandName();
        List<String> params = command.getArgs();
        String paramString = String.join(" ", params);

        commands.add(commandName);
        commands.add(paramString);
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

    public static Map<String, List<String>> runCommand(String commandString, Map<String, String> env) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        if(commandString == null || commandString.isEmpty()) {
            result.put("out", new ArrayList<>());
            result.put("err", new ArrayList<>());
            return result;
        }

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
        ProcessBuilder pb = new ProcessBuilder(commands);
        if(env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }
        Process process = pb.start();

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


    public static Map<String, List<String>> runCustomizeCommand(List<String> commandList, Map<String, String> env) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        if(commandList == null || commandList.isEmpty()) {
            result.put("out", new ArrayList<>());
            result.put("err", new ArrayList<>());
            return result;
        }

        ProcessBuilder pb = new ProcessBuilder(commandList);
        if(env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }
        Process process = pb.start();

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
}
