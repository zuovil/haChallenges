package com.zuovil.haChallenges.common;

import java.io.Serializable;
import java.util.Objects;

public class Response<T> implements Serializable {

    private static final long serialVersionUID = 0L;

    private int code;
    private String raw;
    private T data;

    public Response(int code, String raw, T data) {
        this.code = code;
        this.raw = raw;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}
        Response<?> response = (Response<?>) o;
        return code == response.code && Objects.equals(raw, response.raw) && Objects.equals(data, response.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, raw, data);
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", raw='" + raw + '\'' +
                ", data=" + data +
                '}';
    }
}
