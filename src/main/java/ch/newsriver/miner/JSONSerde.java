package ch.newsriver.miner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.*;

import java.util.Map;

/**
 * Created by eliapalme on 02/06/16.
 */
public class JSONSerde<T> implements Serde<T> {


    final private Serializer<T> serializer;
    final private Deserializer<T> deserializer;

    public JSONSerde(Class<T> tClass) {
        this.serializer = new JsonPOJOSerializer<T>(tClass);
        this.deserializer = new JsonPOJODeserializer<T>(tClass);
    }




    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }



    public class JsonPOJOSerializer<T> implements Serializer<T> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        private Class<T> tClass;

        /**
         * Default constructor needed by Kafka
         */
        public JsonPOJOSerializer(Class<T> tClass) {
            this.tClass = tClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void configure(Map<String, ?> props, boolean isKey) {


        }

        @Override
        public byte[] serialize(String topic, T data) {
            if (data == null)
                return null;

            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new SerializationException("Error serializing JSON message", e);
            }
        }

        @Override
        public void close() {
        }

    }

    public class JsonPOJODeserializer<T> implements Deserializer<T> {
        private ObjectMapper objectMapper = new ObjectMapper();

        private Class<T> tClass;


        public JsonPOJODeserializer(Class<T> tClass) {
            this.tClass = tClass;
        }


        @SuppressWarnings("unchecked")
        @Override
        public void configure(Map<String, ?> props, boolean isKey) {


        }

        @Override
        public T deserialize(String topic, byte[] bytes) {
            if (bytes == null)
                return null;

            T data;
            try {

                data = objectMapper.readValue(bytes, tClass);
            } catch (Exception e) {
                throw new SerializationException(e);
            }

            return data;
        }

        @Override
        public void close() {

        }
    }

}
