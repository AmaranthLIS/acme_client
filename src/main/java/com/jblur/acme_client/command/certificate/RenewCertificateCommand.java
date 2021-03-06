package com.jblur.acme_client.command.certificate;

import com.jblur.acme_client.IOManager;
import com.jblur.acme_client.Parameters;
import com.jblur.acme_client.command.AccountKeyNotFoundException;
import com.jblur.acme_client.manager.CertificateManager;
import com.jblur.acme_client.manager.RegistrationManager;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class RenewCertificateCommand extends CertificateCommand {
    private static final Logger LOG = LoggerFactory.getLogger(RenewCertificateCommand.class);

    private RegistrationManager registrationManager;

    public RenewCertificateCommand(Parameters parameters, RegistrationManager registrationManager)
            throws AccountKeyNotFoundException {
        super(parameters);
        this.registrationManager = registrationManager;
    }

    @Override
    public void commandExecution() {
        List<Certificate> certificatesList = new LinkedList<>();

        List<Certificate> savedCertificateList = getNotExpiredCertificates();
        if (savedCertificateList == null || savedCertificateList.size() == 0) {
            LOG.info("Can not download certificates. Either you haven't generated one or they already expired.");
        } else {
            certificatesList.addAll(savedCertificateList);
        }

        boolean renew = true;

        for (Certificate certificate : certificatesList) {
            try {
                if ((System.currentTimeMillis() + getParameters().getMaxExpirationTime()) < certificate.download().getNotAfter().getTime()) {
                    renew = false;
                    break;
                }
            } catch (AcmeException e) {
                LOG.warn("Can not read certificate: " + certificate.getLocation(), e);
            }
        }

        renew = renew || getParameters().isForce();

        if (renew) {
            try {
                CertificateManager certificateManagement = certificateManagement = new CertificateManager(
                        IOManager.readCSR(getParameters().getCsr()),
                        registrationManager.getRegistration());
                certificatesList.add(certificateManagement.getCertificate());
                error = error || !writeCertificate(certificateManagement, "");
            } catch (AcmeException e) {
                LOG.error("Can not get certificate. Check if your domains of the certificate are verified", e);
                error = true;
            } catch (IOException e) {
                LOG.error("Can not read csr: " + getParameters().getCsr(), e);
                error = true;
            }
        }

        error = error || !writeCertificateList(certificatesList);

    }
}
