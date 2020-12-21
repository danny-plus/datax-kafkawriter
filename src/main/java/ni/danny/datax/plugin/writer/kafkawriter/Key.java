package ni.danny.datax.plugin.writer.kafkawriter;

/**
 * @author danny_ni
 */
public class Key {
    public final static String CONFIG =  "config";
    public final static String COLUMN = "column";
    public final static String NAME = "name";
    public final static String TYPE = "type";

    public final static String DATE_FORMAT = "dateFormat";
    public final static String VALUE = "value";

    public final static String TOPIC = "topic";
    public final static String PARTITION = "partition";
    public final static String MAX_PARTITION = "maxPartition";

    //默认值：256
    public final static String BATCH_SIZE = "batchSize";

    //默认值：32m
    public final static String BATCH_BYTE_SIZE = "batchByteSize";

}
