package cn.edu.sdua.shequ.common;

/**
 * 业务异常
 */
public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
