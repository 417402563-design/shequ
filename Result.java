package cn.edu.sdua.shequ.common;

import lombok.Data;

/**
 * 统一API响应包装
 */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.msg = msg;
        return r;
    }
}
