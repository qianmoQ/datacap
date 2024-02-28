package io.edurt.datacap.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.edurt.datacap.service.entity.DataSetColumnEntity;
import io.edurt.datacap.service.enums.CreatedMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatedModel
{
    private DataSetColumnEntity column;
    private CreatedMode mode;
}
