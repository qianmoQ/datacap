package io.edurt.datacap.service.repository;

import io.edurt.datacap.service.entity.ConfigureEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigureRepository
        extends PagingAndSortingRepository<ConfigureEntity, Long>
{
    /** 按范畴 + 名称定位唯一一条配置（如 EXECUTOR / Local） */
    Optional<ConfigureEntity> findByCategoryAndName(String category, String name);

    /** 列出某范畴下的全部配置（系统配置页面用） */
    List<ConfigureEntity> findAllByCategoryOrderByName(String category);

    boolean existsByCategoryAndName(String category, String name);
}
