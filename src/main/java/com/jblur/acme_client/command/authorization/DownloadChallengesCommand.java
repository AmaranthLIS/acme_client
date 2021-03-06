package com.jblur.acme_client.command.authorization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.jblur.acme_client.Parameters;
import com.jblur.acme_client.command.AccountKeyNotFoundException;
import com.jblur.acme_client.manager.AuthorizationManager;
import org.shredzone.acme4j.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class DownloadChallengesCommand extends AuthorizationCommand {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadChallengesCommand.class);

    public DownloadChallengesCommand(Parameters parameters) throws AccountKeyNotFoundException {
        super(parameters);
    }

    @Override
    public void commandExecution() {
        List<String> failedAuthorizations = new LinkedList<>();

        List<Authorization> authorizationList = getNotExpiredAuthorizations();
        if (authorizationList == null) {
            LOG.error("Can not read file: " +
                    Paths.get(getParameters().getWorkDir(), Parameters.AUTHORIZATION_URI_LIST).toString());
            error = true;
            return;
        }

        for (Authorization authorization : authorizationList) {
            if (getParameters().getDomains() == null || getParameters().getDomains().contains(authorization.getDomain())) {
                try {
                    writeChallengeByAuthorization(new AuthorizationManager(authorization));
                } catch (Exception e) {
                    LOG.error("Can not get challenges for authorization: " + authorization.getLocation()
                            + "\nDomain: " + authorization.getDomain(), e);
                    failedAuthorizations.add(authorization.getLocation().toString());
                }
            }
        }

        error = error || !writeAuthorizationList(authorizationList);

        if (failedAuthorizations.size() > 0) {
            JsonElement failedAuthorizationsJsonElement = getGson().toJsonTree(failedAuthorizations,
                    new TypeToken<List<String>>() {}.getType());
            result.add("failed_authorizations_to_download", failedAuthorizationsJsonElement);
        }
    }
}
