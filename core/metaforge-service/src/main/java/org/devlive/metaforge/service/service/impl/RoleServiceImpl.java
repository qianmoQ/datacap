package org.devlive.metaforge.service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.enums.MenuEnum;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.CodeUtils;
import org.devlive.metaforge.service.entity.MenuEntity;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.initializer.RoleCodeConstraint;
import org.devlive.metaforge.service.record.TreeRecord;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.repository.admin.MenuRepository;
import org.devlive.metaforge.service.service.RoleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class RoleServiceImpl
        implements RoleService
{
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    private final RoleRepository repository;
    private final MenuRepository menuRepository;
    private final RoleCodeConstraint codeConstraint;

    public RoleServiceImpl(RoleRepository repository, MenuRepository menuRepository, RoleCodeConstraint codeConstraint)
    {
        this.repository = repository;
        this.menuRepository = menuRepository;
        this.codeConstraint = codeConstraint;
    }

    @Override
    public CommonResponse<Boolean> checkCode(String code)
    {
        String normalized = StringUtils.trimToNull(code);
        if (normalized == null) {
            return CommonResponse.success(true);
        }
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_INVALID);
        }
        return CommonResponse.success(!repository.existsByCode(normalized));
    }

    @Override
    public CommonResponse<RoleEntity> createRole(RoleEntity configure)
    {
        if (configure.getId() != null) {
            return CommonResponse.failure(ServiceState.USER_ROLE_ID_MISMATCH);
        }
        String code = StringUtils.trimToNull(configure.getCode());
        if (code == null) {
            code = CodeUtils.generateCode(false);
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_INVALID);
        }
        if (repository.existsByCode(code)) {
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_EXISTS);
        }
        try {
            codeConstraint.ensure();
            RoleEntity entity = new RoleEntity();
            entity.setCode(code);
            entity.setName(configure.getName());
            entity.setDescription(configure.getDescription());
            return CommonResponse.success(repository.saveAndFlush(entity));
        }
        catch (DataIntegrityViolationException exception) {
            if (repository.existsByCode(code)) {
                return CommonResponse.failure(ServiceState.USER_ROLE_CODE_EXISTS);
            }
            log.error("Could not create role", exception);
            return CommonResponse.failure("Could not create role");
        }
        catch (SQLException exception) {
            log.error("Could not enforce unique role codes", exception);
            return CommonResponse.failure(ServiceState.USER_ROLE_CODE_CONSTRAINT_FAILED);
        }
    }

    @Override
    public CommonResponse<RoleEntity> updateRole(String originalCode, RoleEntity configure)
    {
        return repository.findByCode(originalCode)
                .map(entity -> {
                    if (configure.getId() != null && !Objects.equals(configure.getId(), entity.getId())) {
                        return CommonResponse.<RoleEntity>failure(ServiceState.USER_ROLE_ID_MISMATCH);
                    }
                    if (configure.getCode() != null && !Objects.equals(configure.getCode(), entity.getCode())) {
                        return CommonResponse.<RoleEntity>failure(ServiceState.USER_ROLE_CODE_IMMUTABLE);
                    }
                    if (configure.getName() != null) {
                        entity.setName(configure.getName());
                    }
                    if (configure.getDescription() != null) {
                        entity.setDescription(configure.getDescription());
                    }
                    return CommonResponse.success(repository.saveAndFlush(entity));
                })
                .orElseGet(() -> CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND));
    }

    @Override
    public CommonResponse<List<TreeRecord>> getMenusByRoleId(Long roleId)
    {
        Optional<RoleEntity> optionalRole = repository.findById(roleId);
        if (!optionalRole.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_ROLE_NOT_FOUND);
        }
        Map<Long, TreeRecord> treeMap = new ConcurrentHashMap<>();
        // All currently available menus
        List<MenuEntity> activedMenus = menuRepository.findAllByType(MenuEnum.VIEW);
        // The current permission has a menu
        RoleEntity role = optionalRole.get();
        Map<Long, MenuEntity> roleMenus = new ConcurrentHashMap<>();
        // Populate own menu
        role.getMenus().forEach(menu -> roleMenus.put(menu.getId(), menu));
        // Into the list
        List<MenuEntity> menuList = StreamSupport.stream(activedMenus.spliterator(), false)
                .sorted(Comparator.comparing(MenuEntity::getParent))
                .collect(Collectors.toList());
        // Sets the parent menu sort
        menuList.forEach(menu -> {
            TreeRecord parent = new TreeRecord();
            if (menu.getParent() == 0) {
                parent.setId(menu.getId());
                parent.setTitle(menu.getName());
                parent.setUrl(menu.getUrl());
                if (!ObjectUtils.isEmpty(roleMenus.get(menu.getId()))) {
                    parent.setChecked(Boolean.TRUE);
                    parent.setSelected(Boolean.TRUE);
                }
                treeMap.put(menu.getId(), parent);
            }
            else {
                // Sub menu
                TreeRecord temp = treeMap.get(menu.getParent());
                List<TreeRecord> childrens = temp.getChildren();
                // Automatically sets a collection of submenus if the current submenu does not belong to the main menu
                if (ObjectUtils.isEmpty(childrens)) {
                    childrens = new ArrayList<>();
                }
                TreeRecord children = new TreeRecord();
                children.setId(menu.getId());
                children.setTitle(menu.getName());
                children.setUrl(menu.getUrl());
                if (!ObjectUtils.isEmpty(roleMenus.get(menu.getId()))) {
                    children.setChecked(Boolean.TRUE);
                    children.setSelected(Boolean.TRUE);
                }
                childrens.add(children);
                temp.setChildren(childrens);
                treeMap.put(temp.getId(), temp);
            }
        });
        // Convert Map data to List
        List<TreeRecord> tree = new ArrayList<>();
        treeMap.keySet().forEach(v -> tree.add(treeMap.get(v)));
        return CommonResponse.success(tree);
    }
}
