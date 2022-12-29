/*
 * Copyright (c) 2022. PortSwigger Ltd. All rights reserved.
 *
 * This code may be used to extend the functionality of Burp Suite Community Edition
 * and Burp Suite Professional, provided that this usage does not violate the
 * license terms for those products.
 */

package example.eventlisteners;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.*;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.*;
import burp.api.montoya.scanner.Scanner;
import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;

import static burp.api.montoya.http.RequestResult.requestResult;
import static burp.api.montoya.http.ResponseResult.responseResult;
import static burp.api.montoya.proxy.RequestFinalInterceptResult.continueWith;
import static burp.api.montoya.proxy.RequestInitialInterceptResult.followUserRules;
import static burp.api.montoya.proxy.ResponseFinalInterceptResult.continueWith;
import static burp.api.montoya.proxy.ResponseInitialInterceptResult.followUserRules;

//Burp will auto-detect and load any class that extends BurpExtension.
public class EventListeners implements BurpExtension
{
    private Logging logging;

    @Override
    public void initialize(MontoyaApi api)
    {
        logging = api.logging();

        Http http = api.http();
        Proxy proxy = api.proxy();
        Extension extension = api.extension();
        Scanner scanner = api.scanner();

        // set extension name
        extension.setName("Event listeners");

        // register a new HTTP handler
        http.registerHttpHandler(new MyHttpHandler());

        // register a new Proxy handler
        proxy.registerRequestHandler(new MyProxyHttpRequestHandler());
        proxy.registerResponseHandler(new MyProxyHttpResponseHandler());

        // register a new Audit Issue handler
        scanner.registerAuditIssueHandler(new MyAuditIssueListenerHandler());

        // register a new extension unload handler
        extension.registerUnloadingHandler(new MyExtensionUnloadHandler());
    }

    private class MyHttpHandler implements HttpHandler
    {
        @Override
        public RequestResult handleHttpRequest(OutgoingHttpRequest outgoingRequest)
        {
            logging.logToOutput("HTTP request to " + outgoingRequest.httpService() + " [" + outgoingRequest.toolSource().toolType().toolName() + "]");

            return requestResult(outgoingRequest, outgoingRequest.annotations());
        }

        @Override
        public ResponseResult handleHttpResponse(IncomingHttpResponse incomingResponse)
        {
            logging.logToOutput("HTTP response from " + incomingResponse.initiatingRequest().httpService() + " [" + incomingResponse.toolSource().toolType().toolName() + "]");

            return responseResult(incomingResponse, incomingResponse.annotations());
        }
    }

    private class MyProxyHttpRequestHandler implements ProxyHttpRequestHandler
    {
        @Override
        public RequestInitialInterceptResult handleReceivedRequest(InterceptedHttpRequest interceptedRequest)
        {
            logging.logToOutput("Initial intercepted proxy request to " + interceptedRequest.httpService());

            return followUserRules(interceptedRequest);
        }

        @Override
        public RequestFinalInterceptResult handleRequestToIssue(InterceptedHttpRequest interceptedRequest)
        {
            logging.logToOutput("Final intercepted proxy request to " + interceptedRequest.httpService());

            return continueWith(interceptedRequest);
        }
    }

    private class MyProxyHttpResponseHandler implements ProxyHttpResponseHandler
    {
        @Override
        public ResponseInitialInterceptResult handleReceivedResponse(InterceptedHttpResponse interceptedResponse)
        {
            logging.logToOutput("Initial intercepted proxy response from " + interceptedResponse.initiatingRequest().httpService());

            return followUserRules(interceptedResponse);
        }

        @Override
        public ResponseFinalInterceptResult handleResponseToReturn(InterceptedHttpResponse interceptedResponse)
        {
            logging.logToOutput("Final intercepted proxy response from " + interceptedResponse.initiatingRequest().httpService());

            return continueWith(interceptedResponse);
        }
    }

    private class MyAuditIssueListenerHandler implements AuditIssueHandler
    {
        @Override
        public void handleNewAuditIssue(AuditIssue auditIssue)
        {
            logging.logToOutput("New scan issue: " + auditIssue.name());
        }
    }

    private class MyExtensionUnloadHandler implements ExtensionUnloadingHandler
    {
        @Override
        public void extensionUnloaded()
        {
            logging.logToOutput("Extension was unloaded.");
        }
    }
}