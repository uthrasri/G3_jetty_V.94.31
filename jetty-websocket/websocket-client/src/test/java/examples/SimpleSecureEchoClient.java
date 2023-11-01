//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package examples;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * Example of a simple Echo Client using TLS against a wss:// destination
 */
public class SimpleSecureEchoClient
{
    public static void main(String[] args)
    {
        String destUri = "wss://echo.websocket.org";
        if (args.length > 0)
        {
            destUri = args[0];
        }

        SslContextFactory ssl = new SslContextFactory.Client();
        ssl.addExcludeProtocols("tls/1.3");
        ssl.setExcludeCipherSuites(); // websocket.org only uses WEAK cipher suites
        HttpClient http = new HttpClient(ssl);
        WebSocketClient client = new WebSocketClient(http);
        SimpleEchoSocket socket = new SimpleEchoSocket();
        try
        {
            http.start();
            client.start();

            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Origin", "https://websocket.org/");
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);

            // wait for closed socket connection.
            socket.awaitClose(5, TimeUnit.SECONDS);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            stop(http);
            stop(client);
        }
    }

    private static void stop(LifeCycle lifeCycle)
    {
        try
        {
            lifeCycle.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
