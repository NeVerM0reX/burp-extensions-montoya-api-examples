/*
 * Copyright (c) 2023. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package example.customscanchecks;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Marker;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.ConsolidationAction;
import burp.api.montoya.scanner.ScanCheck;
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;

import java.util.LinkedList;
import java.util.List;

import static burp.api.montoya.core.ByteArray.byteArray;
import static burp.api.montoya.scanner.AuditResult.auditResult;
import static burp.api.montoya.scanner.ConsolidationAction.KEEP_BOTH;
import static burp.api.montoya.scanner.ConsolidationAction.KEEP_EXISTING;
import static burp.api.montoya.scanner.audit.issues.AuditIssue.auditIssue;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

class MyScanCheck implements ScanCheck
{
    private static final String GREP_STRING = "Page generated by:";
    private static final String INJ_TEST = "|";
    private static final String INJ_ERROR = "Unexpected pipe";

    private final MontoyaApi api;

    MyScanCheck(MontoyaApi api)
    {
        this.api = api;
    }

    @Override
    public AuditResult activeAudit(HttpRequestResponse baseRequestResponse, AuditInsertionPoint auditInsertionPoint)
    {
        HttpRequest checkRequest = auditInsertionPoint.buildHttpRequestWithPayload(byteArray(INJ_TEST)).withService(baseRequestResponse.httpService());

        HttpRequestResponse checkRequestResponse = api.http().sendRequest(checkRequest);

        List<Marker> responseHighlights = getResponseHighlights(checkRequestResponse, INJ_ERROR);

        List<AuditIssue> auditIssueList = responseHighlights.isEmpty() ? emptyList() : singletonList(
                auditIssue(
                        "Pipe injection",
                        "Submitting a pipe character returned the string: " + INJ_ERROR,
                        null,
                        baseRequestResponse.request().url(),
                        AuditIssueSeverity.HIGH,
                        AuditIssueConfidence.CERTAIN,
                        null,
                        null,
                        AuditIssueSeverity.HIGH,
                        checkRequestResponse.withResponseMarkers(responseHighlights)
                )
        );

        return auditResult(auditIssueList);
    }

    @Override
    public AuditResult passiveAudit(HttpRequestResponse baseRequestResponse)
    {
        List<Marker> responseHighlights = getResponseHighlights(baseRequestResponse, GREP_STRING);

        List<AuditIssue> auditIssueList = responseHighlights.isEmpty() ? emptyList() : singletonList(
                auditIssue(
                        "CMS Info Leakage",
                        "The response contains the string: " + GREP_STRING,
                        null,
                        baseRequestResponse.request().url(),
                        AuditIssueSeverity.HIGH,
                        AuditIssueConfidence.CERTAIN,
                        null,
                        null,
                        AuditIssueSeverity.HIGH,
                        baseRequestResponse.withResponseMarkers(responseHighlights)
                )
        );

        return auditResult(auditIssueList);
    }

    @Override
    public ConsolidationAction consolidateIssues(AuditIssue newIssue, AuditIssue existingIssue)
    {
        return existingIssue.name().equals(newIssue.name()) ? KEEP_EXISTING : KEEP_BOTH;
    }

    private static List<Marker> getResponseHighlights(HttpRequestResponse requestResponse, String match)
    {
        List<Marker> highlights = new LinkedList<>();
        String response = requestResponse.response().toString();

        int start = 0;

        while (start < response.length())
        {
            start = response.indexOf(match, start);

            if (start == -1)
            {
                break;
            }

            Marker marker = Marker.marker(start, start+match.length());
            highlights.add(marker);

            start += match.length();
        }

        return highlights;
    }
}