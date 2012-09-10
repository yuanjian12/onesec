/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.net.impl;

import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.onesec.raven.net.ByteBufferPool;
import org.onesec.raven.net.DataProcessor;
import org.onesec.raven.net.PacketProcessor;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataProcessor implements DataProcessor  {
    private final Node owner;
    protected final LoggerHelper logger;
    private final AtomicReference<SelectionKey> keyToProcess = new AtomicReference<SelectionKey>();
    private final AbstractPacketDispatcher packetDispatcher;
    
    private volatile String statusMessage;
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private volatile boolean processingData = false;
    private long startTs;
    private long usingTime = 0;
    private long directRequests = 0;
    private long requestsFromQueue = 0;

    public AbstractDataProcessor(Node owner, AbstractPacketDispatcher packetDispatcher, LoggerHelper logger) {
        this.owner = owner;
        this.packetDispatcher = packetDispatcher;
        this.logger = logger;
    }

    public boolean processData(SelectionKey key) {
        PacketProcessor pp = (PacketProcessor) key.attachment();
        if (pp.changeToProcessing()) {
            if (keyToProcess.compareAndSet(null, key)) {
                key.interestOps(0);
//                if (key.isWritable())
//                    key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                if (!processingData)
                    synchronized(this) {
                        notify();
                    }
                return true;
            } else
                pp.changeToUnprocessing();
        }
        return false;
    }

    public void stop() {
        stopFlag.compareAndSet(false, true);
    }

    public Node getTaskNode() {
        return owner;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void run() {
        if (logger.isInfoEnabled())
            logger.info("Successfully started");
//        final ByteBufferHolder bufferHolder = byteBufferPool.getBuffer(bufferSize);
//        final ByteBuffer buffer = bufferHolder.getBuffer();
        startTs = System.currentTimeMillis();
        try {
            processingData = false;
            while (!stopFlag.get()) {
                SelectionKey key = keyToProcess.get();
                if (key==null) {
                    key = packetDispatcher.getNextKey();
                    if (key!=null)
                        ++requestsFromQueue;
                } else {
                    keyToProcess.set(null);
                    ++directRequests;
                }
                if (key!=null) {
//                    long usingStart = System.currentTimeMillis();
                    processingData = true;
                    try {
                        try {
                            doProcessData(key);
                        } catch (Throwable e) {
                            if (logger.isErrorEnabled())
                                logger.error("Error processig packet", e);
                        }
                    } finally {
                        ((PacketProcessor)key.attachment()).changeToUnprocessing();
//                        long curTime = System.currentTimeMillis();
//                        usingTime += curTime - usingStart;
//                        if (startTs+1000<=curTime) {
//                            logger.debug(String.format(
//                                    "Usage %.2f%% for last %sms, usage time %sms "
//                                    , 100.*usingTime/(curTime-startTs), curTime-startTs, usingTime));
//                            startTs = curTime;
//                            usingTime = 0;
//                        }
                    }
                } else {
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException ex) {
//                        if (logger.isErrorEnabled())
//                            logger.error("Interrupted");
//                    }
                    processingData = false;
                    synchronized(this) {
                        try {
                            wait(5);
                        } catch (InterruptedException ex) {
                            if (logger.isErrorEnabled())
                                logger.error("Interrupted");
                            return;
                        }
                    }
                }
            }
        } finally {
            if (logger.isInfoEnabled())
                logger.info(String.format(
                        "Stopped. DQ Ratio: %.2f; Direct reqs: %s; queue reqs: %s"
                        , 100.*directRequests/(directRequests+requestsFromQueue), directRequests, requestsFromQueue));
        }
    }
    
    protected abstract void doProcessData(SelectionKey key) throws Exception;
}
