/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.schemaregistry.serializer.avro.impl;

import com.google.common.base.Preconditions;
import io.pravega.client.stream.Serializer;
import io.pravega.schemaregistry.serializer.avro.schemas.AvroSchema;
import io.pravega.schemaregistry.client.SchemaRegistryClient;
import io.pravega.schemaregistry.common.Either;
import io.pravega.schemaregistry.contract.data.EncodingInfo;
import io.pravega.schemaregistry.serializer.shared.impl.AbstractDeserializer;
import io.pravega.schemaregistry.serializer.shared.impl.AbstractSerializer;
import io.pravega.schemaregistry.serializer.shared.impl.EncodingCache;
import io.pravega.schemaregistry.serializer.shared.impl.MultiplexedAndGenericDeserializer;
import io.pravega.schemaregistry.serializer.shared.impl.MultiplexedDeserializer;
import io.pravega.schemaregistry.serializer.shared.impl.MultiplexedSerializer;
import io.pravega.schemaregistry.serializer.shared.impl.SerializerConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;

import static io.pravega.schemaregistry.serializer.shared.impl.SerializerFactoryHelper.initForDeserializer;
import static io.pravega.schemaregistry.serializer.shared.impl.SerializerFactoryHelper.initForSerializer;

/**
 * Internal Factory class for Avro serializers and deserializers. 
 */
@Slf4j
public class AvroSerializerFactory {
    /**
     * Creates a typed Avro serializer for the schema. The serializer implementation returned from this method is
     * responsible for interacting with schema registry service and ensures that only valid registered schema can be used.
     *
     * Note: the returned serializer only implements {@link Serializer#serialize(Object)}.
     * It does not implement {@link Serializer#deserialize(ByteBuffer)}.
     *
     * @param config     Serializer Config used for instantiating a new serializer.
     * @param schema     Schema container that encapsulates an AvroSchema
     * @param <T>        Type of event. It accepts either POJO or Avro generated classes and serializes them.
     * @return           A Serializer Implementation that can be used in {@link io.pravega.client.stream.EventStreamWriter} or
     * {@link io.pravega.client.stream.TransactionalEventStreamWriter}.
     */
    public static <T> Serializer<T> serializer(@NonNull SerializerConfig config, @NonNull AvroSchema<T> schema) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");
        SchemaRegistryClient schemaRegistryClient = initForSerializer(config);
        String groupId = config.getGroupId();
        return new AvroSerializer<>(groupId, schemaRegistryClient, schema, config.getEncoder(), config.isRegisterSchema());
    }

    /**
     * Creates a typed avro deserializer for the Schema. The deserializer implementation returned from this method is
     * responsible for interacting with schema registry service and validate the writer schema before using it.
     *
     * Note: the returned serializer only implements {@link Serializer#deserialize(ByteBuffer)}.
     * It does not implement {@link Serializer#serialize(Object)}.
     *
     * @param config     Serializer Config used for instantiating a new serializer.
     * @param schema     Schema container that encapsulates an AvroSchema
     * @param <T>        Type of event. The typed event should be an avro generated class. For generic type use 
     * {@link #genericDeserializer(SerializerConfig, AvroSchema)}
     * @return A deserializer Implementation that can be used in {@link io.pravega.client.stream.EventStreamReader}.
     */
    public static <T> Serializer<T> deserializer(@NonNull SerializerConfig config, @NonNull AvroSchema<T> schema) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");
        SchemaRegistryClient schemaRegistryClient = initForDeserializer(config);
        String groupId = config.getGroupId();

        EncodingCache encodingCache = new EncodingCache(groupId, schemaRegistryClient);

        return new AvroDeserializer<>(groupId, schemaRegistryClient, schema, config.getDecoders(), encodingCache);
    }

    /**
     * Creates a generic avro deserializer. It has the optional parameter for schema.
     * If the schema is not supplied, the writer schema is used for deserialization into {@link GenericRecord}.
     *
     * Note: the returned serializer only implements {@link Serializer#deserialize(ByteBuffer)}.
     * It does not implement {@link Serializer#serialize(Object)}.
     *
     * @param config     Serializer Config used for instantiating a new serializer.
     * @param schema     Schema container that encapsulates an AvroSchema. It can be null to indicate that writer schema should
     *                   be used for deserialization.
     * @return A deserializer Implementation that can be used in {@link io.pravega.client.stream.EventStreamReader}.
     */
    public static Serializer<Object> genericDeserializer(@NonNull SerializerConfig config, @Nullable AvroSchema<Object> schema) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");
        String groupId = config.getGroupId();
        SchemaRegistryClient schemaRegistryClient = initForDeserializer(config);
        EncodingCache encodingCache = new EncodingCache(groupId, schemaRegistryClient);

        return new AvroGenericDeserializer(groupId, schemaRegistryClient, schema, config.getDecoders(), encodingCache);
    }

    /**
     * A multiplexed Avro serializer that takes a map of schemas and validates them individually.
     *
     * @param config  Serializer config.
     * @param schemas map of avro schemas.
     * @param <T>     Base Type of schemas.
     * @return a Serializer which can serialize events of different types for which schemas are supplied.
     */
    public static <T> Serializer<T> multiTypeSerializer(@NonNull SerializerConfig config, @NonNull Map<Class<? extends T>, AvroSchema<T>> schemas) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");

        String groupId = config.getGroupId();
        SchemaRegistryClient schemaRegistryClient = initForSerializer(config);
        Map<Class<? extends T>, AbstractSerializer<T>> serializerMap = getSerializerMap(config, schemas, groupId, schemaRegistryClient);
        return new MultiplexedSerializer<>(serializerMap);
    }

    private static <T> Map<Class<? extends T>, AbstractSerializer<T>> getSerializerMap(
            SerializerConfig config, Map<Class<? extends T>, AvroSchema<T>> schemas, String groupId, 
            SchemaRegistryClient schemaRegistryClient) {
        return schemas
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        x -> new AvroSerializer<>(groupId, schemaRegistryClient, x.getValue(), config.getEncoder(),
                                config.isRegisterSchema())));
    }

    /**
     * A multiplexed Avro Deserializer that takes a map of schemas and deserializes events into those events depending
     * on the object type information in {@link EncodingInfo}.
     *
     * @param config  Serializer config.
     * @param schemas map of avro schemas.
     * @param <T>     Base type of schemas.
     * @return a Deserializer which can deserialize events of different types in the stream into typed objects.
     */
    public static <T> Serializer<T> multiTypeDeserializer(
            @NonNull SerializerConfig config, @NonNull Map<Class<? extends T>, AvroSchema<T>> schemas) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");

        String groupId = config.getGroupId();
        SchemaRegistryClient schemaRegistryClient = initForDeserializer(config);
        EncodingCache encodingCache = new EncodingCache(groupId, schemaRegistryClient);

        Map<String, AbstractDeserializer<T>> deserializerMap = getDeserializerMap(config, schemas, groupId, schemaRegistryClient, encodingCache);
        return new MultiplexedDeserializer<>(groupId, schemaRegistryClient, deserializerMap, config.getDecoders(),
                encodingCache);
    }

    private static <T> Map<String, AbstractDeserializer<T>> getDeserializerMap(
            SerializerConfig config, Map<Class<? extends T>, AvroSchema<T>> schemas, String groupId, 
            SchemaRegistryClient schemaRegistryClient, EncodingCache encodingCache) {
        return schemas
                .values().stream().collect(Collectors.toMap(x -> x.getSchemaInfo().getType(),
                        x -> new AvroDeserializer<>(groupId, schemaRegistryClient, x, config.getDecoders(), encodingCache)));
    }

    /**
     * A multiplexed Avro Deserializer that takes a map of schemas and deserializes events into those events depending
     * on the object type information in {@link EncodingInfo}.
     *
     * @param config  Serializer config.
     * @param schemas map of avro schemas.
     * @param <T>     Base type of schemas.
     * @return a Deserializer which can deserialize events of different types in the stream into typed objects or a generic
     * object
     */
    public static <T> Serializer<Either<T, Object>> typedOrGenericDeserializer(
            @NonNull SerializerConfig config, @NonNull Map<Class<? extends T>, AvroSchema<T>> schemas) {
        Preconditions.checkArgument(config.isWriteEncodingHeader(), "Events should be tagged with encoding ids.");

        String groupId = config.getGroupId();
        SchemaRegistryClient schemaRegistryClient = initForDeserializer(config);

        EncodingCache encodingCache = new EncodingCache(groupId, schemaRegistryClient);

        Map<String, AbstractDeserializer<T>> deserializerMap = getDeserializerMap(config, schemas, groupId, schemaRegistryClient, encodingCache);
        AbstractDeserializer<Object> genericDeserializer = new AvroGenericDeserializer(groupId, schemaRegistryClient,
                null, config.getDecoders(), encodingCache);
        return new MultiplexedAndGenericDeserializer<>(groupId, schemaRegistryClient, deserializerMap, genericDeserializer,
                config.getDecoders(), encodingCache);
    }
}
