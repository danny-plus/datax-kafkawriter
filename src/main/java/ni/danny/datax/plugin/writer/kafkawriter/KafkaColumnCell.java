package ni.danny.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.base.BaseObject;

import java.io.Serializable;

/**
 * @author danny_ni
 */
public class KafkaColumnCell extends BaseObject implements Serializable{
    private String name;
    private ColumnType type;
    private String dateFormat;
    private String value;

    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getValue() {
        return value;
    }

    private KafkaColumnCell(Builder builder){
        this.dateFormat = builder.dateFormat;
        this.name = builder.name;
        this.type = builder.type;
        this.value = builder.value;
    }

    public static class Builder{
        private String name;
        private ColumnType type;
        private String dateFormat;
        private String value;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(ColumnType type) {
            this.type = type;
            return this;
        }

        public Builder setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }



        public KafkaColumnCell build(){
            return new KafkaColumnCell(this);
        }
    }


}
