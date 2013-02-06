/*
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.container;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.atmosphere.container.version.TomcatWebSocket;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class TomcatWebSocketHandler extends StreamInbound {

    private static final Logger logger = LoggerFactory.getLogger(TomcatWebSocketHandler.class);

    private final WebSocketProcessor webSocketProcessor;
    private final AtmosphereRequest request;
    private final AtmosphereFramework framework;
    private WebSocket webSocket;
    private final int webSocketWriteTimeout;

    public TomcatWebSocketHandler(AtmosphereRequest request, AtmosphereFramework framework, WebSocketProcessor webSocketProcessor) {
        this.request = request;
        this.framework = framework;
        this.webSocketProcessor = webSocketProcessor;

        String s = framework.getAtmosphereConfig().getInitParameter(ApplicationConfig.WEBSOCKET_IDLETIME);
        if (s != null) {
            webSocketWriteTimeout = Integer.valueOf(1);
        } else {
            webSocketWriteTimeout = -1;
        }
    }

    @Override
    protected void onOpen(WsOutbound outbound) {
        logger.trace("WebSocket.onOpen.");
        webSocket = new TomcatWebSocket(outbound, framework.getAtmosphereConfig());
        try {
            webSocketProcessor.open(webSocket, request, AtmosphereResponse.newInstance(framework.getAtmosphereConfig(), request, webSocket));
        } catch (Exception e) {
            logger.warn("failed to connect to web socket", e);
        }
    }

    @Override
    protected void onClose(int closeCode) {
        request.destroy();
        if (webSocketProcessor == null) return;

        webSocketProcessor.close(webSocket, closeCode);
    }

    protected void onTextMessage(CharBuffer message) throws IOException {
        logger.trace("WebSocket.onMessage");
        webSocketProcessor.invokeWebSocketProtocol(webSocket, message.toString());
    }

    @Override
    protected final void onBinaryData(InputStream is) throws IOException {
        logger.trace("WebSocket.onBynaryStream");
        webSocketProcessor.invokeWebSocketProtocol(webSocket, is);
    }

    @Override
    protected final void onTextData(Reader r) throws IOException {
        logger.trace("WebSocket.onTextStream");
        webSocketProcessor.invokeWebSocketProtocol(webSocket, r);
    }

    @Override
    public int getReadTimeout(){
        return webSocketWriteTimeout;
    }
}
