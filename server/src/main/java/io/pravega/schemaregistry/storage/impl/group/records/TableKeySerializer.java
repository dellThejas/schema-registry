/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.schemaregistry.storage.impl.group.records;

import io.pravega.common.io.serialization.VersionedSerializer;
import io.pravega.common.util.ByteArraySegment;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TableKeySerializer extends VersionedSerializer.MultiType<TableRecords.TableKey> {

    @Override
    protected void declareSerializers(Builder builder) {
        // Unused values (Do not repurpose!):
        // - 0: Unsupported Serializer.
        builder.serializer(TableRecords.SchemaIdKey.class, 1, TableRecords.SchemaIdKey.SERIALIZER)
               .serializer(TableRecords.ValidationPolicyKey.class, 2, TableRecords.ValidationPolicyKey.SERIALIZER)
               .serializer(TableRecords.Etag.class, 3, TableRecords.Etag.SERIALIZER)
               .serializer(TableRecords.GroupPropertyKey.class, 4, TableRecords.GroupPropertyKey.SERIALIZER)
               .serializer(TableRecords.SchemaFingerprintKey.class, 5, TableRecords.SchemaFingerprintKey.SERIALIZER)
               .serializer(TableRecords.EncodingInfoRecord.class, 6, TableRecords.EncodingInfoRecord.SERIALIZER)
               .serializer(TableRecords.EncodingIdRecord.class, 7, TableRecords.EncodingIdRecord.SERIALIZER)
               .serializer(TableRecords.LatestEncodingIdKey.class, 8, TableRecords.LatestEncodingIdKey.SERIALIZER)
               .serializer(TableRecords.CodecTypeKey.class, 9, TableRecords.CodecTypeKey.SERIALIZER)
               .serializer(TableRecords.CodecTypesKey.class, 10, TableRecords.CodecTypesKey.SERIALIZER)
               .serializer(TableRecords.LatestSchemasKey.class, 11, TableRecords.LatestSchemasKey.SERIALIZER)
               .serializer(TableRecords.IndexTypeVersionToIdKey.class, 12, TableRecords.IndexTypeVersionToIdKey.SERIALIZER)
               .serializer(TableRecords.VersionDeletedRecord.class, 13, TableRecords.VersionDeletedRecord.SERIALIZER)
               .serializer(TableRecords.SchemaIdChunkKey.class, 14, TableRecords.SchemaIdChunkKey.SERIALIZER);
    }

    /**
     * Serializes the given {@link TableRecords.TableKey} to a {@link ByteBuffer}.
     *
     * @param value The {@link TableRecords.TableKey} to serialize.
     * @return An array that contains the serialized key.
     */
    @SneakyThrows(IOException.class)
    public byte[] toBytes(TableRecords.TableKey value) {
        ByteArraySegment s = serialize(value);
        return s.getCopy();
    }
    
    /**
     * Deserializes the given buffer into a {@link TableRecords.TableKey} instance.
     *
     * @param buffer buffer to deserialize into key.
     * @return A new {@link TableRecords.TableKey} instance from the given serialization.
     */
    @SneakyThrows(IOException.class)
    public TableRecords.TableKey fromBytes(byte[] buffer) {
        return deserialize(new ByteArraySegment(buffer));
    }
}
