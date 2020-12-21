package ni.danny.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @author danny_ni
 */

public enum KafkaWriterErrorCode implements ErrorCode {
    ILLEGAL_VALUE("KafkaWriteErrorCode-01", "您填写的参数值不合法."),
    CONF_ERROR("KafkaWriteErrorCode-00", "您的配置错误."),

    ;


    private final String code;

    private final String description;

    private KafkaWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s]. ", this.code,
                this.description);
    }
}
