/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.schemaregistry.contract.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Defines different types of codecs that can be used for encoding the data. Codecs can be used for compressing data 
 * or encode the serialized data in the desired format.
 * A codec type and schema version combination uniquely identifies encoding format for the serialized data which is expressed
 * as {@link EncodingInfo}.
 * If a custom codec type which is not identified by the enum is desired by the application, it can be specified using
 * {@link CodecType#custom} with {@link CodecType#customTypeName} and optionally user supplied key value properties via
 * {@link CodecType#properties}.  
 */
public enum CodecType {
        None,
        Snappy,
        GZip,
        Custom;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String customTypeName;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private Map<String, String> properties;

    /**
     * Method to create a custom CodecType with a specific customTypeName and properties. 
     * 
     * @param customTypeName Name of custom codec type. 
     * @param properties User supplied key value properties. 
     * @return CodecType.Custom with custom type name and properties set. 
     */
    public static CodecType custom(String customTypeName, Map<String, String> properties) {
        CodecType custom = CodecType.Custom;
        custom.setCustomTypeName(customTypeName);
        custom.setProperties(properties);
        return custom;
    }
}
