package com.zuovil.haChallenges.common;

import java.util.ArrayList;
import java.util.List;

public class Command {
    private final String       commandName;
    private final List<String> args;

    public Command(String name) {
        this.commandName = name;
        this.args = new ArrayList<>();
    }

    public static Command fromInput(String input) {
        if (input == null || input.isEmpty()) return null;

        List<String> words = splitShellWords(input);
        if (words.isEmpty()) return null;

        Command cmd = new Command(words.get(0));
        for (int i = 1; i < words.size(); i++) cmd.addArg(words.get(i));
        return cmd;
    }

    private static List<String> splitShellWords(String input) {
        // 状态: NORMAL 引号外的正常字符 IN_SINGLE 单引号 IN_DOUBLE 双引号 ESCAPE 反斜杠转义
        final int NORMAL = 0, IN_SINGLE = 1, IN_DOUBLE = 2, ESCAPE = 3;

        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int state = NORMAL;

        boolean lastTokenEndedWithQuote = false; // 上一个 token 是否以真实引号结束
        boolean lastCharWasSpace = false;       // 折叠连续空格
        int quoteDepth = 0;                     // 引号层级计数器

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (state) {
                case ESCAPE:
                    cur.append(c);
                    state = NORMAL;
                    lastCharWasSpace = false;
                    if(c == '\'' || c == '"') {
                        quoteDepth ++;
                    }
                    break;

                case IN_SINGLE:
                    if (c == '\'') {
                        state = NORMAL;
                        lastTokenEndedWithQuote = true;
                        quoteDepth--;
                    } else {
                        // '\"test script\"'
                        cur.append(c);
                        lastCharWasSpace = false;
                        //if (c == ' ') quoteDepth = Math.max(quoteDepth, 1);
                    }
                    break;

                case IN_DOUBLE:
                    if (c == '"') {
                        state = NORMAL;
                        lastTokenEndedWithQuote = true;
                        quoteDepth--;
                        if(quoteDepth == 0) {
                            tokens.add(cur.toString());
                            cur.setLength(0);
                        }
                        // echo "world test" "example""script"
                    } else if (c == '\\') {
                        if (i + 1 < input.length()) {
                            char nxt = input.charAt(i + 1);
                            if (nxt == '"' || nxt == '\'') {
                                // '\"test script\"'
                                // cat "/tmp/owl/\"f 21\""
                                if(quoteDepth <= 1) {
                                    quoteDepth ++;
                                } else {
                                    quoteDepth --;
                                }
                                // /不能转义单引号，因此额外排除单引号的情况，即只允许转义双引号
                                // 单引号模式的核心：里面不能出现单引号；在 '...' 中出现的所有字符都必须被完全原样保留，不允许转义，不允许解释，不允许扩展。【初始规则】
                                if(nxt == '"' ) {
                                    cur.append(nxt);
                                    i++;
                                } else {
                                    cur.append('\\');
                                }

                            } else if(nxt == '\\') {
                                cur.append(nxt);
                                i++;
                            } else {
                                cur.append('\\');
                            }
                        } else {
                            cur.append('\\');
                        }
                        lastCharWasSpace = false;
                    } else {
                        cur.append(c);
                        lastCharWasSpace = false;
                        // if (c == ' ') quoteDepth = Math.max(quoteDepth, 1);
                    }
                    break;

                case NORMAL:
                    if (c == '\\') {
                        state = ESCAPE;
                    } else if (c == '\'') {
                        if (lastTokenEndedWithQuote && lastCharWasSpace && cur.length() == 0){
                            tokens.add(" ");
                            cur.setLength(0);
                        }
                        state = IN_SINGLE;
                        lastTokenEndedWithQuote = false;
                        quoteDepth++;
                    } else if (c == '"') {
                        if (lastTokenEndedWithQuote && lastCharWasSpace && cur.length() == 0) {
                            tokens.add(" ");
                            cur.setLength(0);
                        }
                        state = IN_DOUBLE;
                        lastTokenEndedWithQuote = false;
                        quoteDepth++;
                    } else if (Character.isWhitespace(c)) {
                        if (quoteDepth > 0) {
                            cur.append(c);
                        } else if (!lastCharWasSpace && cur.length() > 0) {
                            tokens.add(cur.toString());
                            cur.setLength(0);
                        }
                        lastCharWasSpace = true;
                    } else {
                        // echo "hello"  "world's"  shell""script
                        // 排除首个，防止出现echo hello-> " hello"
                        // echo shell     world(全部一般字符）
                        if(tokens.size() > 1 && lastCharWasSpace && cur.length() == 0) {
                            tokens.add(" ");
                            cur.setLength(0);
                        }
                        cur.append(c);
                        lastCharWasSpace = false;
                        lastTokenEndedWithQuote = false;
                    }
                    break;
            }
        }

        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }

    public void addArg(String arg) {args.add(arg);}

    public String getCommandName() {return commandName;}

    public List<String> getArgs() {return args;}
}