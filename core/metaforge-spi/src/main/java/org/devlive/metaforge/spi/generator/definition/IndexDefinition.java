package org.devlive.metaforge.spi.generator.definition;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class IndexDefinition
{
    private String name;
    private List<String> columns = Lists.newArrayList();
    private boolean unique;
    private String comment;
}
