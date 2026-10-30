package org.devlive.metaforge.server.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.plugin.Plugin;
import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.entity.SourceEntity;
import org.devlive.metaforge.service.repository.SourceRepository;
import org.devlive.metaforge.service.service.impl.MetadataServiceImpl;
import org.devlive.metaforge.spi.PluginService;
import org.devlive.metaforge.spi.generator.definition.TableDefinition;
import org.devlive.metaforge.spi.model.ManagementCapabilities;
import org.devlive.metaforge.spi.model.ManagementPage;
import org.devlive.metaforge.spi.model.ManagementRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MetadataCapabilityTest
{
    @Test
    public void profilesComeFromThePluginAndDoNotOpenAConnection()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("Redis");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("Redis")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(ManagementCapabilities.forJdbc("redis"));
        MetadataServiceImpl service = new MetadataServiceImpl(manager, repository);

        CommonResponse<ManagementCapabilities> result = service.getCapabilities("source-code");
        assertTrue(result.getStatus());
        assertEquals("key-value", result.getData().getKind());
        assertFalse(result.getData().supports("column:modify"));
        assertFalse(service.changeColumn("source-code", "0", "key", TableDefinition.create("0", "key")).getStatus());
        assertFalse(service.createTable("source-code", "0", TableDefinition.create("0", "key")).getStatus());
        assertFalse(service.getColumns("source-code", "0", "key").getStatus());
        verify(adapter, never()).connect(any());
        verify(adapter, never()).changeColumn(any(), any());
        verify(adapter, never()).createTable(any(), any());
        verify(adapter, never()).getColumns(any(), any(), any());
    }

    @Test
    public void nativeReadsUseTheNativeAdapterAndDownloadsRemainCapabilityChecked()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginService adapter = mock(PluginService.class);
        SourceEntity source = new SourceEntity();
        source.setType("Redis");
        when(repository.findByCode("source-code")).thenReturn(Optional.of(source));
        when(manager.getPlugin("Redis")).thenReturn(Optional.of(plugin));
        when(plugin.getService(PluginService.class)).thenReturn(adapter);
        when(adapter.managementCapabilities()).thenReturn(new ManagementCapabilities("key-value", Set.of("key:list", "key:read", "namespace:list")));
        ManagementPage expected = new ManagementPage();
        when(adapter.managementList(any(), any())).thenReturn(expected);
        MetadataServiceImpl service = new MetadataServiceImpl(manager, repository);
        ManagementRequest request = new ManagementRequest();
        assertTrue(service.managementList("source-code", request).getStatus());
        verify(adapter).managementList(any(), any());
        verify(adapter, never()).getTables(any(), any());
        assertFalse(service.managementDownload("source-code", request).getStatus());
        verify(adapter, never()).managementDownload(any(), any());
    }

    @Test
    public void invalidBatchBudgetsFailBeforeOpeningAPluginOrConnection()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        ManagementRequest request = new ManagementRequest();
        request.setLimit(201);
        assertFalse(new MetadataServiceImpl(manager, repository).managementList("source-code", request).getStatus());
        verifyNoInteractions(repository, manager);
    }

    @Test
    public void missingSourcesFailBeforeLookingUpPlugins()
    {
        SourceRepository repository = mock(SourceRepository.class);
        PluginManager manager = mock(PluginManager.class);
        when(repository.findByCode("missing")).thenReturn(Optional.empty());
        assertFalse(new MetadataServiceImpl(manager, repository).getCapabilities("missing").getStatus());
        verifyNoInteractions(manager);
    }
}
