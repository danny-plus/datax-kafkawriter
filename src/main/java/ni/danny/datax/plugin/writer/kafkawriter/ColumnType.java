package ni.danny.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * @author danny_ni
 */

public enum ColumnType {
    VALUE("value"),
    DATE("date"),
    CONST("const")
    ;
    private String typeName;
    ColumnType(String typeName){this.typeName = typeName; }

    public static ColumnType getByTypeName(String typeName) {
        if(StringUtils.isBlank(typeName)){
            throw DataXException.asDataXException(KafkaWriterErrorCode.ILLEGAL_VALUE,
                    String.format("kafkawriter 不支持该类型:%s, 目前支持的类型是:%s", typeName, Arrays.asList(values())));
        }
        for (ColumnType columnType : values()) {
            if (StringUtils.equalsIgnoreCase(columnType.typeName, typeName.trim())) {
                return columnType;
            }
        }

        throw DataXException.asDataXException(KafkaWriterErrorCode.ILLEGAL_VALUE,
                String.format("kafkawriter 不支持该类型:%s, 目前支持的类型是:%s", typeName, Arrays.asList(values())));
    }

    @Override
    public String toString() {
        return this.typeName;
    }
}
