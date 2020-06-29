/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.schemaregistry.server.rest.auth;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.pravega.auth.AuthConstants;
import io.pravega.auth.AuthException;
import io.pravega.auth.AuthHandler;
import io.pravega.auth.AuthenticationException;
import io.pravega.auth.ServerConfig;
import io.pravega.controller.server.rpc.auth.StrongPasswordProcessor;
import io.pravega.controller.server.rpc.auth.UserPrincipal;
import io.pravega.schemaregistry.server.rest.ServiceConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class BasicAuthHandler implements AuthHandler {

    private final ConcurrentHashMap<String, PravegaACls> userMap;
    private final StrongPasswordProcessor encryptor;

    public BasicAuthHandler() {
        userMap = new ConcurrentHashMap<>();
        encryptor = StrongPasswordProcessor.builder().build();
    }

    private void loadPasswordFile(String userPasswordFile) {
        log.debug("Loading {}", userPasswordFile);

        try (FileReader reader = new FileReader(userPasswordFile);
             BufferedReader lineReader = new BufferedReader(reader)) {
            String line;
            while (!Strings.isNullOrEmpty(line = lineReader.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] userFields = line.split(":");
                if (userFields.length >= 2) {
                    String acls;
                    if (userFields.length == 2) {
                        acls = "";
                    } else {
                        acls = userFields[2];
                    }
                    userMap.put(userFields[0], new PravegaACls(userFields[1], getAcls(acls)));
                }
            }
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    @Override
    public String getHandlerName() {
        return AuthConstants.BASIC;
    }

    @Override
    public Principal authenticate(String token) throws AuthException {
        String[] parts = parseToken(token);
        String userName = parts[0];
        char[] password = parts[1].toCharArray();

        try {
            if (userMap.containsKey(userName) && encryptor.checkPassword(password, userMap.get(userName).encryptedPassword)) {
                return new UserPrincipal(userName);
            }
            throw new AuthenticationException("User authentication exception");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.warn("Exception during password authentication", e);
            throw new AuthenticationException(e);
        } finally {
            Arrays.fill(password, '0'); // Zero out the password for security.
        }
    }

    @Override
    public Permissions authorize(String resource, Principal principal) {
        String userName = principal.getName();

        if (Strings.isNullOrEmpty(userName) || !userMap.containsKey(userName)) {
            throw new CompletionException(new AuthenticationException(userName));
        }
        return authorizeForUser(userMap.get(userName), resource);
    }

    @Override
    public void initialize(ServerConfig serverConfig) {
        loadPasswordFile(((ServiceConfig) serverConfig).getUserPasswordFile());
    }

    private static String[] parseToken(String token) {
        String[] parts = new String(Base64.getDecoder().decode(token), Charsets.UTF_8).split(":", 2);
        Preconditions.checkArgument(parts.length == 2, "Invalid authorization token");
        return parts;
    }

    private Permissions authorizeForUser(PravegaACls pravegaACls, String resource) {
        Permissions result = Permissions.NONE;

        /*
         *  `*` Means a wildcard.
         *  If It is a direct match, return the ACLs.
         *  If it is a partial match, the target has to end with a `/`
         */
        for (PravegaAcl acl : pravegaACls.acls) {
            // Separating into different blocks, to make the code more understandable.
            // It makes the code look a bit strange, but it is still simpler and easier to decipher than what it be
            // if we combine the conditions.

            if (acl.isResource(resource)) {
                // Example: resource = "mygroup", acl-resource = "mygroup"

                result = acl.permissions;
                break;
            }

            if (acl.isResource("/*") && !resource.contains("/")) {
                // Example: resource = "mygroup", acl-resource ="/*"
                result = acl.permissions;
                break;
            }
            
            if (acl.resourceEndsWith("/") && acl.resourceStartsWith(resource)) {
                result = acl.permissions;
                break;
            }

            // Say, resource is mygroup/schemas. ACL specifies permission for mygroup/*.
            // Auth should return the the ACL's permissions in that case.
            if (resource.contains("/") && !resource.endsWith("/")) {
                String[] values = resource.split("/");
                String res = null;
                for (String value : values) {
                    res = res == null ? value : res + "/" + value;
                    if (acl.isResource(res + "/*")) {
                        result = acl.permissions;
                        break;
                    }
                } 
            }

            if (acl.isResource("*") && acl.hasHigherPermissionsThan(result)) {
                result = acl.permissions;
                break;
            }
        }
        return result;
    }

    private List<PravegaAcl> getAcls(String aclString) {
        return Arrays.stream(aclString.split(";")).map(acl -> {
            String[] splits = acl.split(",");
            if (splits.length == 0) {
                return null;
            }
            String resource = splits[0];
            String aclVal = "READ";
            if (splits.length >= 2) {
                aclVal = splits[1];

            }
            return new PravegaAcl(resource,
                    Permissions.valueOf(aclVal));
        }).collect(Collectors.toList());
    }

    @Data
    private static class PravegaACls {
        private final String encryptedPassword;
        private final List<PravegaAcl> acls;
    }

    @Data
    private static class PravegaAcl {
        private final String resourceRepresentation;
        private final Permissions permissions;

        boolean isResource(String resource) {
            return resourceRepresentation.equals(resource);
        }

        boolean resourceEndsWith(String resource) {
            return resourceRepresentation.endsWith(resource);
        }

        boolean resourceStartsWith(String resource) {
            return resourceRepresentation.startsWith(resource);
        }

        boolean hasHigherPermissionsThan(Permissions input) {
            return this.permissions.ordinal() > input.ordinal();
        }
    }
}