package ni.danny.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.exception.DataXException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class KafkaWriterHelp {
    private final static Logger LOG = LoggerFactory.getLogger(KafkaWriterHelp.class);
    //TODO 检查配置

    //解析COLUMN
    private static KafkaColumnCell parseOneColumn(String columnName){
        Map<String,String> columnMap = new HashMap<>();
        columnMap.put(Key.NAME,columnName);
        columnMap.put(Key.VALUE,"");
        columnMap.put(Key.TYPE,"string");
        columnMap.put(Key.DATE_FORMAT,"");
        return parseOneColumn(columnMap);
    }

    private static KafkaColumnCell parseOneColumn(Map<String,String> columnInfo){
        ColumnType columnType = ColumnType.VALUE;
        if(StringUtils.isNotBlank(columnInfo.get(Key.TYPE))){
            columnType = ColumnType.getByTypeName(columnInfo.get(Key.TYPE));
        }

        String columnName = columnInfo.get(Key.NAME);
        String value = columnInfo.get(Key.VALUE);
        String dateFormat = columnInfo.get(Key.DATE_FORMAT);

        KafkaColumnCell column = new KafkaColumnCell.Builder()
                .setDateFormat(dateFormat)
                .setType(columnType)
                .setValue(value)
                .setName(columnName)
                .build();
        return column;
    }


    public static List<KafkaColumnCell> parseColumn(List<Object> columnList){
        List<KafkaColumnCell> list = new ArrayList<KafkaColumnCell>();
        for(Object column:columnList){
            if(column instanceof String){
                list.add(KafkaWriterHelp.parseOneColumn((String)column));
            }else if(column instanceof Map){
                list.add(KafkaWriterHelp.parseOneColumn((Map<String,String>)column));
            }else{
                throw DataXException.asDataXException(KafkaWriterErrorCode.ILLEGAL_VALUE,
                        String.format("kafkawriter 无法解析column"));
            }
        }
        return list;
    }

    /**
     * 解析TOPIC
     * @param topicList
     * @return
     */
    public static List<KafkaTopic> parseTopic(List<Object> topicList){
        List<KafkaTopic> list = new ArrayList<>();
        for(Object topic:topicList){
            if(topic instanceof String){
                list.add(parseOneTopic((String)topic));
            }else if(topic instanceof Map){
                list.add(parseOneTopic((Map<String,String>)topic));
            }else{
                throw DataXException.asDataXException(KafkaWriterErrorCode.ILLEGAL_VALUE,
                        String.format("kafkawriter 无法解析Topic"));
            }
        }
        return list;
    }

    private static KafkaTopic parseOneTopic(String topic){
        Map<String,String> map = new HashMap<>(3);
        map.put(Key.NAME,topic);
        map.put(Key.PARTITION,"-1");
        map.put(Key.MAX_PARTITION,"-1");
        return parseOneTopic(map);
    }

    private static KafkaTopic parseOneTopic(Map<String,String> topic){
        String topicName = topic.get(Key.NAME);
        int partition = -1;
        if(StringUtils.isNotBlank(topic.get(Key.PARTITION))
        && StringUtils.isNumeric(topic.get(Key.PARTITION))){
            partition = Integer.parseInt(topic.get(Key.PARTITION));
        }
        int maxPartition = -1;
        if(StringUtils.isNotBlank(topic.get(Key.MAX_PARTITION))
                && StringUtils.isNumeric(topic.get(Key.MAX_PARTITION))){
            maxPartition = Integer.parseInt(topic.get(Key.MAX_PARTITION));
        }

        KafkaTopic kafkaTopic = new KafkaTopic.Builder()
                .setName(topicName)
                .setPartation(partition)
                .setMaxPartation(maxPartition)
                .build();

        return kafkaTopic;
    }
}
