package org.devlive.metaforge.plugin.natived.kafka;

import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.configure.FieldType;
import org.devlive.metaforge.plugin.configure.IConfigure;
import org.devlive.metaforge.plugin.configure.IConfigureExecutor;
import org.devlive.metaforge.plugin.configure.IConfigureExecutorField;
import org.devlive.metaforge.plugin.configure.IConfigureField;
import org.devlive.metaforge.plugin.configure.IConfigureFieldName;
import org.devlive.metaforge.plugin.configure.IConfigureFieldType;
import org.devlive.metaforge.plugin.configure.IConfigurePipelineType;
import org.devlive.metaforge.plugin.configure.RunProtocol;

import java.util.Arrays;

public class KafkaPlugin
        extends Plugin
{
    @Override
    public IConfigure getConfigure()
    {
        return IConfigure.of("Kafka",
                Arrays.asList(
                        IConfigureField.builder()
                                .field(IConfigureFieldName.name)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .message("name is a required field, please be sure to enter")
                                .build(),
                        IConfigureField.builder()
                                .field(IConfigureFieldName.host)
                                .type(IConfigureFieldType.String)
                                .required(true)
                                .value("127.0.0.1:9092")
                                .message("host is a required field, please be sure to enter")
                                .build()),
                Arrays.asList(
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.INPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("bootstrap.servers")
                                                .origin("host")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("topic")
                                                .origin("topic")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("Kafka topic name. If there are multiple topics, use , to split, for example: \"tpc1,tpc2\".")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("pattern")
                                                .origin("pattern")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(false)
                                                .description("If pattern is set to true,the regular expression for a pattern of topic names to read from. All topics in clients with names that match the specified regular expression will be subscribed by the consumer.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("group")
                                                .origin("consumer.group")
                                                .override(true)
                                                .input(true)
                                                .value("MetaForge-Pipeline-Group")
                                                .description("Kafka consumer group id, used to distinguish different consumer groups.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("checkpoint")
                                                .origin("commit_on_checkpoint")
                                                .override(true)
                                                .input(true)
                                                .width(45)
                                                .type(FieldType.SWITCH)
                                                .value(true)
                                                .description("If true the consumer's offset will be periodically committed in the background.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("format")
                                                .origin("format")
                                                .override(true)
                                                .input(true)
                                                .value("json")
                                                .description("Data format. The default format is json. Optional text format. The default field separator is \", \". If you customize the delimiter, add the \"field_delimiter\" option.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("delimiter")
                                                .origin("field_delimiter")
                                                .override(true)
                                                .input(true)
                                                .value(",")
                                                .description("Customize the field delimiter for data format.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("start_mode")
                                                .origin("start_mode")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.SELECT)
                                                .value("group_offsets")
                                                .defaultValues(Arrays.asList("earliest", "group_offsets", "latest", "specific_offsets", "timestamp"))
                                                .description("The initial consumption pattern of consumers,there are several types: [earliest],[group_offsets],[latest],[specific_offsets],[timestamp]")
                                                .build())),
                        IConfigureExecutor.of("Seatunnel", IConfigurePipelineType.OUTPUT, RunProtocol.NONE,
                                Arrays.asList(
                                        IConfigureExecutorField.builder()
                                                .field("bootstrap.servers")
                                                .origin("host")
                                                .required(true)
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("topic")
                                                .origin("topic")
                                                .required(true)
                                                .override(true)
                                                .input(true)
                                                .description("Kafka Topic.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("semantic")
                                                .origin("semantic")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.SELECT)
                                                .value("NON")
                                                .defaultValues(Arrays.asList("EXACTLY_ONCE", "AT_LEAST_ONCE", "NON"))
                                                .description("Semantics that can be chosen EXACTLY_ONCE/AT_LEAST_ONCE/NON, default NON.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("partition_key_fields")
                                                .origin("partition_key_fields")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("Configure which fields are used as the key of the kafka message.\n"
                                                        + "\n"
                                                        + "For example, if you want to use value of fields from upstream data as key, you can assign field names to this property.\n"
                                                        + "\n"
                                                        + "If there are multiple values, each value needs to be one row.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("partition")
                                                .origin("partition")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.NUMBER)
                                                .description("We can specify the partition, all messages will be sent to this partition.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("assign_partitions")
                                                .origin("assign_partitions")
                                                .override(true)
                                                .input(true)
                                                .type(FieldType.TEXT)
                                                .description("We can decide which partition to send based on the content of the message. The function of this parameter is to distribute information.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("transaction_prefix")
                                                .origin("transaction_prefix")
                                                .override(true)
                                                .input(true)
                                                .description("If semantic is specified as EXACTLY_ONCE, the producer will write all messages in a Kafka transaction. Kafka distinguishes different transactions by different transactionId. This parameter is prefix of kafka transactionId, make sure different job use different prefix.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("format")
                                                .origin("format")
                                                .override(true)
                                                .input(true)
                                                .value("json")
                                                .description("Data format. The default format is json. Optional text format. The default field separator is \", \". If you customize the delimiter, add the \"field_delimiter\" option.")
                                                .build(),
                                        IConfigureExecutorField.builder()
                                                .field("delimiter")
                                                .origin("field_delimiter")
                                                .override(true)
                                                .input(true)
                                                .value(",")
                                                .description("Customize the field delimiter for data format.")
                                                .build()))));
    }
}
