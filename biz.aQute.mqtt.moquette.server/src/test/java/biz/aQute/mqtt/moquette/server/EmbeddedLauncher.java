/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package biz.aQute.mqtt.moquette.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import aQute.lib.io.IO;
import io.moquette.broker.Server;
import io.moquette.broker.config.FileResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;

/**
 * Simple example of how to embed the broker in another project
 * */
public final class EmbeddedLauncher {

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
        	ByteBuf payload = msg.getPayload();
        	byte[] data = new byte[payload.readableBytes()];
        	payload.readBytes(data);
        	String s = new String(data, StandardCharsets.UTF_8);
        	
            System.out.println("Received on topic: " + msg.getTopicName() + " content: " + s);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        IResourceLoader loader = new FileResourceLoader(IO.getFile("resources/config/config.properties"));
        final IConfig classPathConfig = new ResourceLoaderConfig(loader);

        final Server mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = Collections.singletonList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);

        Thread.sleep(100000000);
    }

    private EmbeddedLauncher() {
    }
}
