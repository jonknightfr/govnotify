/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */
/**
 * jon.knight@forgerock.com
 *
 * A node that registers a mobile device for SNS push notifications
 */


package org.forgerock.openam.auth.nodes;

import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.SendEmailResponse;

import com.google.inject.assistedinject.Assisted;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;

import java.util.ResourceBundle;
import java.util.*;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.annotations.adapters.Password;

import com.google.common.collect.ImmutableList;
import org.forgerock.util.i18n.PreferredLocales;

import com.sun.identity.sm.RequiredValueValidator;


/**
 * An authentication node integrating with iProov face recognition solution.
 */

@Node.Metadata(outcomeProvider = GovNotifyNode.OutcomeProvider.class,
        configClass = GovNotifyNode.Config.class)
public class GovNotifyNode implements Node {

    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        default String apiKey() { return "apiKey"; }

        @Attribute(order = 200)
        default String templateId() { return "templateId"; }

        @Attribute(order = 300)
        default String mailAttr() { return "mail"; }
    }

    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "GovNotifyNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private static final String BUNDLE = GovNotifyNode.class.getName().replace(".", "/");
    private ResourceBundle resourceBundle;

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public GovNotifyNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }


    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        debug.error("GovNotifyNode started.");
        NotificationClient client = new NotificationClient(config.apiKey());

        String mailAddr = context.sharedState.get(config.mailAttr()).asString();
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("username", context.sharedState.get("username").asString());

        try {
            SendEmailResponse response = client.sendEmail(
                    config.templateId(),
                    mailAddr,
                    personalisation,
                    ""
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        this.resourceBundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        String username = context.sharedState.get(USERNAME).asString();
        String phone = getTelephoneNumber(coreWrapper.getIdentity(username,
                coreWrapper.convertRealmPathToRealmDn(context.sharedState.get(REALM).asString())));
        String twilioPhoneNumber = config.twilio_PHONE_NUMBER();
        if (config.twilio_WHATSAPP()) {
            phone = "whatsapp:" + phone;
            twilioPhoneNumber = "whatsapp:" + twilioPhoneNumber;
        }
        debug.error(username + " Phone Number " + phone);
        debug.error(" Twilio Number " + twilioPhoneNumber);


        String authToken = new String(config.twilio_AUTH_TOKEN());
        Twilio.init(config.twilio_ACCOUNT_SID(), authToken);
        try {
            String response = Message.creator(new PhoneNumber(phone), new PhoneNumber(twilioPhoneNumber), config.message() +
                " " + context.sharedState.get(ONE_TIME_PASSWORD).asString()).create().getSid();
            debug.error("Tracking SID is " + response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NodeProcessException(resourceBundle.getString("Unable to send HOTP code " + e ));
            //Todo need some way of signifying the message didn't go through. Either with NodeProcessException or
            //a custom outcome provider

        }
        */
        return goTo("ok").build();
    }




    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }


    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = GovNotifyNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome("ok", bundle.getString("ok")));
        }
    }
}
