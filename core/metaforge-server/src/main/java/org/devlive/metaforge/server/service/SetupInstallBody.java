package org.devlive.metaforge.server.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupInstallBody
{
    private String username;
    private String password;
    private String name;
    /**
     * Optional target database. Null keeps the current datasource.
     * `embedded` installs into the local H2 file store, `mysql` into an
     * external server; the chosen datasource is persisted to
     * ./config/application.properties so the next boot uses it.
     */
    private DatabaseConfigure database;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseConfigure
    {
        private String type;
        private String host;
        private Integer port;
        private String database;
        private String username;
        private String password;
    }
}
