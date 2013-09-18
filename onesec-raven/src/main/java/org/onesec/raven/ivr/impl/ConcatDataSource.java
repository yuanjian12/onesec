/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.ivr.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.control.PacketSizeControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.*;
import org.onesec.raven.ivr.*;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class ConcatDataSource extends PushBufferDataSource implements AudioStream
{
    public static final int SOURCE_WAIT_TIMEOUT = 100;
    public static final int WAIT_STATE_TIMEOUT = 2000;

    private final LoggerHelper logger;
    private final String contentType;
    private final ExecutorService executorService;
    private final CodecManager codecManager;
    private final ConcatDataStream[] streams;
    private final Queue<Buffer> buffers = new ConcurrentLinkedQueue<Buffer>();
    private final AtomicReference<SourceProcessor> sourceProcessorRef = 
            new AtomicReference<SourceProcessor>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Node owner;
    private final AtomicBoolean streamThreadRunning = new AtomicBoolean(false);
    private final AudioFormat format;
    private final int rtpPacketSize;
    private final long packetSizeInMillis;
    private final BufferCache bufferCache;
    private final Codec codec;
//    private String logPrefix;
    private int bufferCount;

    public ConcatDataSource(String contentType
            , ExecutorService executorService
            , CodecManager codecManager
            , Codec codec
            , int rtpPacketSize
            , int rtpInitialBufferSize
            , int rtpMaxSendAheadPacketsCount
            , Node owner
            , BufferCache bufferCache
            , LoggerHelper logger)
    {
        this.contentType = contentType;
        this.executorService = executorService;
        this.codecManager = codecManager;
        this.owner = owner;
        this.rtpPacketSize = rtpPacketSize;
        this.packetSizeInMillis = rtpPacketSize / 8;
        this.format = codec.getAudioFormat();
        this.bufferCache = bufferCache;
        this.codec = codec;
        this.logger = new LoggerHelper(logger, "AudioStream. ");
        
        bufferCount = 0;
        Buffer silentBuffer = bufferCache.getSilentBuffer(executorService, owner, codec, rtpPacketSize);
        streams = new ConcatDataStream[]{new ConcatDataStream(
                buffers, this, owner, rtpPacketSize, codec, rtpMaxSendAheadPacketsCount, silentBuffer, logger)};
    }

    public void addSource(DataSource source) {
        replaceSourceProcessor(new SourceProcessor(source));
    }

    public void addSource(String key, long checksum, DataSource source) {
        replaceSourceProcessor(new SourceProcessor(source, key, checksum));
    }

    public void addSource(InputStreamSource source) {
        if (source!=null)
            addSource(new ContainerParserDataSource(codecManager, source, contentType));
    }

    public void addSource(String key, long checksum, InputStreamSource source) {
        if (source!=null)
            addSource(key, checksum, new ContainerParserDataSource(codecManager, source, contentType));
    }

    private SourceProcessor replaceSourceProcessor(final SourceProcessor newSourceProcessor)
    {
        final SourceProcessor oldSp = sourceProcessorRef.getAndSet(newSourceProcessor);
        if (oldSp!=null) {
            oldSp.stop();
            executorService.executeQuietly(new AbstractTask(owner, "Stopping source processing") {
                @Override public void doRun() throws Exception {
                    oldSp.close();
                }
            });
        }
        buffers.clear();
        if (newSourceProcessor!=null)
            executorService.executeQuietly(new AbstractTask(owner, logger.logMess("Starting processing new source")){
                @Override public void doRun() throws Exception {
                    newSourceProcessor.start();
                }
            });
        return newSourceProcessor;
    }

    public String getLogPrefix() {
        return logger.getPrefix();
    }

//    public void setLogPrefix(String logPrefix) {
//        this.logPrefix = logPrefix;
//    }

    public DataSource getDataSource() {
        return this;
    }

    public Format getFormat() {
        return format;
    }

    public boolean isPlaying() {
        SourceProcessor sp = sourceProcessorRef.get();
        return (sp!=null && sp.isProcessing()) || !buffers.isEmpty();
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return ContentDescriptor.RAW;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void start() throws IOException {
        if (started.compareAndSet(false, true))
            try {
                executorService.execute(streams[0]);
            } catch (ExecutorServiceException ex) {
                throw new IOException(ex);
            }
    }

    @Override
    public void stop() throws IOException {
    }

    public void close()  {
        if (!stopped.compareAndSet(false, true))
            return;
        replaceSourceProcessor(null);
        buffers.clear();
        try {
            while (streamThreadRunning.get())
                TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            if (logger.isErrorEnabled())
                logger.error("ConcatDataSource close operation was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isClosed() {
        return stopped.get();
    }

    public void reset() {
        replaceSourceProcessor(null);
    }

    void setStreamThreadRunning(boolean streamThreadRunning) {
        this.streamThreadRunning.set(streamThreadRunning);
    }

    @Override
    public Object getControl(String controlClass) {
        return null;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Time getDuration() {
        return DURATION_UNKNOWN;
    }

    public Node getTaskNode() {
        return owner;
    }

//    String logMess(String mess, Object... args) {
//        return (logPrefix==null? "" : logPrefix)+"AudioStream. "+String.format(mess, args);
//    }
    
    long getPacketSizeInMillis() {
        return packetSizeInMillis;
    }

    class SourceProcessor implements BufferTransferHandler {
        private final DataSource source;
        private final AtomicBoolean stopProcessing = new AtomicBoolean(Boolean.FALSE);
        private final Lock lock = new ReentrantLock();
        private final String sourceKey;
        private final long sourceChecksum;
        private final ConcatDataStream concatStream;
        private final boolean realTime;
        private Collection<Buffer> cache;

//        private PushBufferDataSource dataSource;
//        private Processor processor;
        private TranscoderDataSource transcoder;
        private long startTs;
        private long firstBufferTs;

        public SourceProcessor(DataSource source) {
            this.source = source;
            this.sourceChecksum = 0l;
            this.sourceKey = null;
            this.concatStream = streams[0];
            this.realTime = source instanceof RealTimeDataSourceMarker;
        }

        public SourceProcessor(DataSource source, String sourceKey, long sourceChecksum) {
            this.source = source;
            this.sourceKey = sourceKey;
            this.sourceChecksum = sourceChecksum;
            this.concatStream = streams[0];
            this.realTime = source instanceof RealTimeDataSourceMarker;
        }

        public boolean isProcessing(){
            return !stopProcessing.get();
        }

        public boolean isRealTime() {
            return realTime;
        }

        public void start(){
            if (lock.tryLock()) try {
                if (!stopProcessing.get()) try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Processing new source...");
                        logger.debug("Detected real time source");
                    }
                    startTs = System.currentTimeMillis();
                    if (!applyBuffersFromCache())
                        readBuffersFromSource();
                    if (logger.isDebugEnabled())
                        logger.debug("Source initialization time (ms) - " + (System.currentTimeMillis() - startTs));
                }catch(Throwable e){
                    if (logger.isErrorEnabled())
                        logger.error("Error processing source", e);
                }
            } finally {
                lock.unlock();
            }
        }
        
        private boolean applyBuffersFromCache() {
            if (sourceKey==null)
                return false;
            Buffer[] cachedBuffers = bufferCache.getCachedBuffers(sourceKey, sourceChecksum, codec, rtpPacketSize);
            if (cachedBuffers==null)
                return false;
            buffers.addAll(Arrays.asList(cachedBuffers));
            stopProcessing.set(true);
            if (logger.isDebugEnabled())
                logger.debug("Buffers applied from the cache (number of buffers - {})", cachedBuffers.length);
            return true;
        }

        private void readBuffersFromSource() throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("Reading buffers from source");
            transcoder = new TranscoderDataSource(codecManager, prepareSource(source), format, logger);
            transcoder.connect();
            PacketSizeControl packetSizeControl =
                    (PacketSizeControl) transcoder.getControl(PacketSizeControl.class.getName());
            if (packetSizeControl != null) {
                if (logger.isDebugEnabled())
                    logger.debug("Found packet control so setting up packet size for encoder");
                packetSizeControl.setPacketSize(rtpPacketSize);
            }
            transcoder.getStreams()[0].setTransferHandler(this);
            transcoder.start();
        }
        
        private PushBufferDataSource prepareSource(DataSource source) {
            PushBufferDataSource res;
            if (source instanceof PullDataSource)
                source = new ContainerParserDataSource(codecManager, source);
            if (source instanceof PullBufferDataSource)
                res = new PullToPushConverterDataSource((PullBufferDataSource)source, executorService, owner);
            else 
                res = (PushBufferDataSource) source;
            return res;
        }

        public void stop() {
            stopProcessing.set(true);
        }

        public void close(){
            stopProcessing.set(true);
            concatStream.sourceClosed(this);
            try {
                if (lock.tryLock(2000, TimeUnit.MILLISECONDS)) try {
                    if (transcoder!=null) {
                        transcoder.stop();
                        transcoder.disconnect();
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error("Error stopping source processor", e);
            }
        }

        public void transferData(PushBufferStream stream) {
            try {
                if (stopProcessing.get())
                    return;
                Buffer buffer = new Buffer();
                stream.read(buffer);
                if (buffer.isDiscard())
                    return;
                boolean theEnd = false;
                if (buffer.isEOM()) {
                    buffer.setEOM(false);
                    close();
                    theEnd = true;
                }
                if (firstBufferTs==0) 
                    concatStream.sourceInitialized(this);
                if (isRealTime())
                    buffer.setTimeStamp(System.currentTimeMillis());
                buffers.add(buffer);
                if (sourceKey!=null){
                    if (cache==null)
                        cache = new LinkedList<Buffer>();
                    cache.add(buffer);
                    if (theEnd) {
                        if (logger.isDebugEnabled())
                            logger.debug(String.format(
                                    "Caching buffers: key - %s, codec - %s, packetSize - %s"
                                    , sourceKey, codec, rtpPacketSize));
                        bufferCache.cacheBuffers(sourceKey, sourceChecksum, codec, rtpPacketSize, cache);
                    }
                }
                ++bufferCount;
                if (theEnd && logger.isDebugEnabled()) 
                    logger.debug("Source processing time: {} ms", System.currentTimeMillis()-startTs);
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error("Error reading buffer from source", e);
                close();
            }
        }
    }
}
