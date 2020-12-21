package ni.danny.datax.plugin.writer.kafkawriter;


import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author danny_ni
 */
public class KafkaWriter extends Writer {
    public static class Job extends Writer.Job{
        private static final Logger log = LoggerFactory.getLogger(Job.class);

        private Configuration configuration = null;

        @Override
        public void init() {
            this.configuration = super.getPluginJobConf();
            Map configMap = this.configuration.getMap(Key.CONFIG);
            Properties props = new Properties();
            props.putAll(configMap);
            //TODO 尝试连接(admin)
            try{
                AdminClient kafkaAdmin = AdminClient.create(props);
                List<KafkaTopic> topics = KafkaWriterHelp.parseTopic(this.configuration.getList(Key.TOPIC));
                //检查TOPIC信息 ,并获取MAX_PARTITION数据,添加至参数
                List<String> topicsName = topics.stream().map(f->f.getName()).collect(Collectors.toList());
                DescribeTopicsResult topicsResult = kafkaAdmin.describeTopics(topicsName);
                Map<String, TopicDescription> topicDescMap = topicsResult.all().get();
                List<KafkaTopic> newTopics = new ArrayList<>(topics.size());
                for(KafkaTopic topic:topics){
                    if(topicDescMap.get(topic.getName())!=null){
                        TopicDescription topicDescription = topicDescMap.get(topic.getName());
                        int maxPartitions = topicDescription.partitions().size();
                        if(maxPartitions <= topic.getPartation()){
                            throw DataXException
                                    .asDataXException(
                                            KafkaWriterErrorCode.CONF_ERROR,
                                            String.format(
                                                    "KAFKA配置错误，TOPIC 指定PARTITION超出范围"));
                        }
                        KafkaTopic.Builder topicBuilder = new KafkaTopic.Builder();
                        topicBuilder.setName(topic.getName());
                        topicBuilder.setPartation(topic.getPartation());
                        topicBuilder.setMaxPartation(maxPartitions);
                        newTopics.add(topicBuilder.build());
                    }
                }

                this.configuration.remove(Key.TOPIC);
                this.configuration.set(Key.TOPIC,newTopics);

            }catch (Exception ex){
                throw DataXException
                        .asDataXException(
                                KafkaWriterErrorCode.CONF_ERROR,
                                String.format(
                                        "KAFKA配置错误，或TOPIC 请检查您的配置并作出修改."));
            }
        }

        /**
         * 根据TOPIC进行拆分
         * @param i
         * @return
         */
        @Override
        public List<Configuration> split(int i) {
            List<Configuration> splitResultConfigs = new ArrayList<>();
            List<KafkaTopic> topics = KafkaWriterHelp.parseTopic(this.configuration.getList(Key.TOPIC));
            for(KafkaTopic topic:topics){
                Configuration splitConfig = this.configuration.clone();
                splitConfig.remove(Key.TOPIC);
                splitConfig.set(Key.TOPIC,new ArrayList<KafkaTopic>(){{add(topic);}});
                splitResultConfigs.add(splitConfig);
            }
            return splitResultConfigs;
        }

        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task{
        private static final Logger log = LoggerFactory.getLogger(Job.class);
        private Configuration configuration = null;
        private List<KafkaTopic> topics = null;
        private List<KafkaColumnCell> columns = null;
        private int totalColumnNum = 0;
        private int constColumnNum = 0;
        private int valueColumnNum = 0;

        protected int batchSize;
        protected int batchByteSize;

        private Gson gson = null;

        private KafkaProducer<?,?> kafkaProducer;

        @Override
        public void init() {
            this.configuration = super.getPluginJobConf();
            Map configMap = this.configuration.getMap(Key.CONFIG);
            Properties props = new Properties();
            props.putAll(configMap);
            this.gson = new GsonBuilder().create();
            //解析TOPIC
            this.topics = KafkaWriterHelp.parseTopic(this.configuration.getList(Key.TOPIC));
            //解析COLUMN
            this.columns = KafkaWriterHelp.parseColumn(this.configuration.getList(Key.COLUMN));

            this.totalColumnNum = this.columns.size();
            for(KafkaColumnCell columnCell:this.columns){
                if(ColumnType.CONST.equals(columnCell.getType())){
                    this.constColumnNum ++;
                }else {
                    this.valueColumnNum ++;
                }
            }

            this.batchSize = this.configuration.getInt(Key.BATCH_SIZE,Constant.DEFAULT_BATCH_SIZE);
            this.batchByteSize = this.configuration.getInt(Key.BATCH_BYTE_SIZE,Constant.DEFAULT_BATCH_BYTE_SIZE);


            this.kafkaProducer = new KafkaProducer(props);
        }

        @Override
        public void startWrite(RecordReceiver recordReceiver) {
            Record record = null;
            List<Record> writeBuffer = new ArrayList<Record>(this.batchSize);
            int bufferBytes = 0;
            while ((record = recordReceiver.getFromReader()) != null) {
                if(record.getColumnNumber() != this.valueColumnNum){
                    throw DataXException
                            .asDataXException(
                                    KafkaWriterErrorCode.CONF_ERROR,
                                    String.format(
                                            "列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
                                            record.getColumnNumber(),
                                            this.valueColumnNum));
                }

                writeBuffer.add(record);
                bufferBytes += record.getMemorySize();

                if(writeBuffer.size() >= batchSize ||bufferBytes >= batchByteSize){
                    sendBatchRecord(writeBuffer);
                    writeBuffer.clear();
                    bufferBytes = 0;
                }
            }
            if(!writeBuffer.isEmpty()){
                sendBatchRecord(writeBuffer);
                writeBuffer.clear();
                bufferBytes = 0;
            }
        }

        protected void sendBatchRecord(List<Record> writeBuffer){
            List<Map<String,Object>> list = convertRecordsToMsg(writeBuffer);

            String msg = this.gson.toJson(list);
            for(KafkaTopic topic:this.topics){
                ProducerRecord producerRecord =
                        new ProducerRecord(topic.getName(),topic.getPartation(),topic.getPartation(),msg);

                this.kafkaProducer.send(producerRecord);
            }
        }

        protected List<Map<String,Object>> convertRecordsToMsg(List<Record> writeBuffer){
            List<Map<String,Object>> list = new ArrayList<>();
            for(Record record: writeBuffer){
                list.add(convertRecordToMsg(record));
            }
            return list;
        }

        protected Map<String,Object> convertRecordToMsg(Record record){
            Map<String,Object> map = new HashMap<>(this.totalColumnNum);
            int i = 0;
            for(KafkaColumnCell columnCell:this.columns){
                switch (columnCell.getType()){
                    case CONST: map.put(columnCell.getName(),columnCell.getValue()) ; break;
                    case DATE:
                        String dateTimeStr = "";
                        try {
                            Date date = record.getColumn(i).asDate();
                            DateTime dateTime = new DateTime(date.getTime());
                            dateTimeStr = dateTime.toString(columnCell.getDateFormat());
                        } catch (DataXException e) {
                            throw DataXException
                                    .asDataXException(
                                            KafkaWriterErrorCode.ILLEGAL_VALUE,
                                            String.format(
                                                    "日期解析失败：%s",
                                                    record.getColumn(i)));
                        }

                        map.put(columnCell.getName(),dateTimeStr);
                        i++; break;
                    default:
                        map.put(columnCell.getName(),record.getColumn(i).getRawData());
                        i++; break;
                }
            }
            return map;
        }


        @Override
        public void destroy() {
            if(this.kafkaProducer !=null){
                this.kafkaProducer.close();
            }
        }
    }
}
