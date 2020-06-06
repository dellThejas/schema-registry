/*
 * Pravega Schema Registry APIs
 * REST APIs for Pravega Schema Registry.
 *
 * OpenAPI spec version: 0.0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.pravega.schemaregistry.contract.generated.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

/**
 * Serialization format enum that lists different serialization formats supported by the service. To use additional formats, use serializationFormat.Custom and supply customTypeName.
 */
@ApiModel(description = "Serialization format enum that lists different serialization formats supported by the service. To use additional formats, use serializationFormat.Custom and supply customTypeName.")

public class SerializationFormat   {
  /**
   * Gets or Sets serializationFormat
   */
  public enum SerializationFormatEnum {
    AVRO("Avro"),
    
    PROTOBUF("Protobuf"),
    
    JSON("Json"),
    
    ANY("Any"),
    
    CUSTOM("Custom");

    private String value;

    SerializationFormatEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SerializationFormatEnum fromValue(String text) {
      for (SerializationFormatEnum b : SerializationFormatEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("serializationFormat")
  private SerializationFormatEnum serializationFormat = null;

  @JsonProperty("customTypeName")
  private String customTypeName = null;

  public SerializationFormat serializationFormat(SerializationFormatEnum serializationFormat) {
    this.serializationFormat = serializationFormat;
    return this;
  }

  /**
   * Get serializationFormat
   * @return serializationFormat
   **/
  @JsonProperty("serializationFormat")
  @ApiModelProperty(required = true, value = "")
  @NotNull
  public SerializationFormatEnum getSerializationFormat() {
    return serializationFormat;
  }

  public void setSerializationFormat(SerializationFormatEnum serializationFormat) {
    this.serializationFormat = serializationFormat;
  }

  public SerializationFormat customTypeName(String customTypeName) {
    this.customTypeName = customTypeName;
    return this;
  }

  /**
   * Get customTypeName
   * @return customTypeName
   **/
  @JsonProperty("customTypeName")
  @ApiModelProperty(value = "")
  public String getCustomTypeName() {
    return customTypeName;
  }

  public void setCustomTypeName(String customTypeName) {
    this.customTypeName = customTypeName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SerializationFormat serializationFormat = (SerializationFormat) o;
    return Objects.equals(this.serializationFormat, serializationFormat.serializationFormat) &&
        Objects.equals(this.customTypeName, serializationFormat.customTypeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serializationFormat, customTypeName);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SerializationFormat {\n");
    
    sb.append("    serializationFormat: ").append(toIndentedString(serializationFormat)).append("\n");
    sb.append("    customTypeName: ").append(toIndentedString(customTypeName)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
