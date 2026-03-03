/*
 * Copyright (c) 2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package example.contextmenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.regex.Pattern;


public class MyContextMenuItemsProvider implements ContextMenuItemsProvider
{
    private final MontoyaApi api;
    private final Preferences preferences;
    

    // temporary fix until Burp Settings API becomes stable
    public final static List<String> DEFAULT_knownUselessHeaders = List.of(
        "Accept", 
        "Accept-Language",
        "Accept-Encoding",
        "X-Pwnfox-Color", 
        "Priority", 
        "Te", 
        "User-Agent", 
        "X-Requested-With", 
        "Vary", 
        "Access-Control-Allow-Headers", 
        "Access-Control-Allow-Methods", 
        "X-Xss-Protection", 
        "X-Content-Type-Options", 
        "Access-Control-Expose-Headers", 
        "Alt-Svc", 
        "Via", 
        "Server", 
        "Sec-Ch-Ua", 
        "Sec-Ch-Ua-Mobile", 
        "Sec-Ch-Ua-Platform", 
        "Upgrade-Insecure-Requests", 
        "Sec-Fetch-Site", 
        "Sec-Fetch-Mode", 
        "Sec-Fetch-Dest", 
        "Origin", 
        "Referer"
    );
    
    public final static List<String> DEFAULT_knownUselessCookies = List.of(
        "^_[\\w_]*$",
        "^optimizely\\w+$",
        "^AMP_[\\w_]+$",
        "^ajs_\\w+_id$",
        "^GOOG\\w+$"
    );

    public MyContextMenuItemsProvider(MontoyaApi api)
    {
        this.api = api;
        this.preferences = api.persistence().preferences();
    }

    public List<Pattern> getUselessCookies() {
        String stored = preferences.getString("uselessCookies");
        if (stored == null || stored.isEmpty()) {
            stored = String.join(",", DEFAULT_knownUselessCookies);
        }
        return Arrays.stream(stored.split(","))
                     .map(String::trim)
                     .map(Pattern::compile)
                     .collect(Collectors.toList());
    }

    public List<HttpHeader> getUselessHeaders() {
        String stored = preferences.getString("uselessHeaders");
        if (stored == null || stored.isEmpty()) {
            stored = String.join(",", DEFAULT_knownUselessHeaders);
        }
        return Arrays.stream(stored.split(","))
                     .map(String::trim)
                     .map(HttpHeader::httpHeader)
                     .collect(Collectors.toList());
    }

    private HttpRequest redact(HttpRequest request) {
        if (request.hasHeader("Authorization")) {
            request = request.withUpdatedHeader("Authorization", request.headerValue("Authorization").replaceAll(" .+$", " [REDACTED]"));
        }
        if (request.hasHeader("X-Authorization")) {
            request = request.withUpdatedHeader("X-Authorization", request.headerValue("X-Authorization").replaceAll(" .+$", " [REDACTED]"));
        }
        if (request.hasHeader("Cookie")) {
            List<String> newCookies = Arrays.stream(request.headerValue("Cookie").split(";"))
                .map(String::trim)
                .map(s -> s.split("=", 2))
                .filter(a -> getUselessCookies().stream().noneMatch(p -> p.matcher(a[0]).matches()))
                .map(a -> (a.length > 1) ? (a[0] + "=" + a[1]) : a[0])
                .collect(Collectors.toList());
            
            if (newCookies.isEmpty()) {
                request = request.withRemovedHeader("Cookie");
            } else {
                request = request.withUpdatedHeader("Cookie", String.join("; ", newCookies));
            }
        }
        return request.withRemovedHeaders(getUselessHeaders());
    }

    // TODO: figure out what happens if we get the Set-Cookie header (which can appear more than once)
    private HttpResponse redact(HttpResponse response) {
        return response.withRemovedHeaders(getUselessHeaders());
    }

    private String formatRequestResponse(HttpRequestResponse requestResponse) {
        String report = "HTTP request:\r\n<cb>\r\n" + redact(requestResponse.request()).toString() + "\r\n</cb>\r\n\r\n";
        if (requestResponse.response() != null) {
            report += "HTTP response:\r\n<cb>\r\n" + redact(requestResponse.response()).toString() + "\r\n</cb>\r\n";
        } else {
            report += "No response was received.\r\n";
        }
        return report;
    }

    private void copyRequestResponsesToClipboard(ContextMenuEvent event) {
        List<HttpRequestResponse> requestResponses;

        if (event.messageEditorRequestResponse().isPresent()) {
            requestResponses = Collections.singletonList(event.messageEditorRequestResponse().get().requestResponse());
        } else {
            requestResponses = event.selectedRequestResponses();
        }

        String report = "";
        for (HttpRequestResponse requestResponse : requestResponses) {
            report += formatRequestResponse(requestResponse) + "\r\n";
        }
        report = report.replaceAll("[\r\n]+$", "");

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(report), null);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event)
    {
        List<Component> menuItemList = new ArrayList<>();
        JMenuItem retrieveRequestItem = new JMenuItem("Copy for report");

        retrieveRequestItem.addActionListener(l -> copyRequestResponsesToClipboard(event));
        menuItemList.add(retrieveRequestItem);
        return menuItemList;
    }
}
