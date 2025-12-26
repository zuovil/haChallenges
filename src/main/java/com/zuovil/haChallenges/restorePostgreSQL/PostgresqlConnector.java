package com.zuovil.haChallenges.restorePostgreSQL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;

public class PostgresqlConnector {

    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/mydb";
    private static final String DATABASE_USER = "postgres";
    private static final String DATABASE_PASSWORD = "";

    private final static ObjectMapper mapper = new ObjectMapper();

    private static Connection connection = null;

    {
        try {
            // 加载PostgreSQL JDBC驱动程序
            Class.forName("org.postgresql.Driver");
            // 创建数据库连接
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("host", "localhost");
    }

    public String query(String query) {
        if (query == null || query.isEmpty()) {
            return "{}";
        }

        try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet         result      = statement.executeQuery(query);
            ResultSetMetaData metaData    = result.getMetaData();  //获取列集
            int               columnCount = metaData.getColumnCount(); //获取列的数量

            ArrayNode arrayNode = mapper.createArrayNode();
            result.last();
            if(result.getRow() == 0) {
                return "{}";
            }
            result.beforeFirst();
            while (result.next()) {
                ObjectNode node = arrayNode.objectNode();
                for (int i = 0; i < columnCount; i++) { //循环列
                    String     columnName  = metaData.getColumnName(i + 1); //通过序号获取列名,起始值为1
                    String     columnValue = result.getString(columnName);  //通过列名获取值.如果列值为空,columnValue为null,不是字符型
                    node.put(columnName, columnValue);
                }
                arrayNode.add(node);
            }

            result.close();

            return arrayNode.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "{}";
        }

    }

    public static void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
